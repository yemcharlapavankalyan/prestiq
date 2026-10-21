"""Source-grounded RAG QA engine with citation verification and anti-hallucination guardrails."""

import logging
import re
from typing import List, Optional, Tuple
from app.config import settings
from app.core.exceptions import RateLimitExceededError, ScholarshipNotFoundError
from app.models.document import DocumentChunk
from app.models.schemas import ChatResponse, Citation
from app.providers.factory import ProviderFactory, provider_factory
from app.storage.base import VectorStore

logger = logging.getLogger(__name__)

# Injection patterns to detect and block
INJECTION_PATTERNS = [
    r"ignore\s+(all\s+)?(previous|prior)\s+instructions",
    r"you\s+are\s+now\s+(an?\s+)?admin",
    r"system\s+prompt",
    r"developer\s+mode",
    r"disregard\s+(the\s+)?rules",
    r"bypass\s+restrictions",
    r"output\s+all\s+(your\s+)?instructions",
]

# Patterns where users request official eligibility decisions
ELIGIBILITY_PATTERNS = [
    r"\bam\s+i\s+eligible\b",
    r"\bdo\s+i\s+qualify\b",
    r"\bwill\s+i\s+get\s+(this|the)\s+scholarship\b",
    r"\bcan\s+you\s+(assess|determine|evaluate|decide)\s+my\s+eligibility\b",
    r"\bcalculate\s+my\s+(eligibility|score)\b",
]

SYSTEM_PROMPT = """You are PrestIQ's verified Scholarship Information Assistant.
Your mission is to provide accurate, truthful answers to students' questions based strictly on the verified official documentation excerpts provided below.

MANDATORY RULES:
1. Grounding: Rely EXCLUSIVELY on the provided excerpts. Do NOT hallucinate, assume, or extrapolate facts not present in the excerpts.
2. Honest Uncertainty: If the excerpts do not contain the answer, explicitly reply: "The provided official guidelines do not specify this information. Please consult the official scholarship link for verified details."
3. Citations: Every factual claim or answer MUST include a source citation footnote formatted as [Source X] or [Source X, Page Y] corresponding to the indexed excerpt number.
4. Eligibility Disclaimer: Never declare or decide whether a student is eligible. Official eligibility assessments are calculated deterministically exclusively by PrestIQ's Eligibility Service.
5. Tone: Be concise, clear, and encouraging.
"""


class QAEngine:
    """End-to-end question answering engine for scholarship guidelines."""

    def __init__(
        self,
        vector_store: VectorStore,
        factory: ProviderFactory = provider_factory
    ):
        self.vector_store = vector_store
        self.factory = factory

    def check_prompt_injection(self, text: str) -> bool:
        """Scan text for adversarial prompt injection attempts."""
        lower = text.lower()
        for pattern in INJECTION_PATTERNS:
            if re.search(pattern, lower):
                return True
        return False

    def check_eligibility_inquiry(self, text: str) -> bool:
        """Check if user is asking the AI to make an official eligibility assessment."""
        lower = text.lower()
        for pattern in ELIGIBILITY_PATTERNS:
            if re.search(pattern, lower):
                return True
        return False

    async def answer_question(
        self,
        scholarship_id: int,
        question: str
    ) -> ChatResponse:
        """
        Synthesize source-grounded answer with verified citations.
        """
        # 1. Guard Check: Prompt Injection Defense
        if self.check_prompt_injection(question):
            logger.warning("Adversarial prompt injection attempt detected on scholarship %d", scholarship_id)
            return ChatResponse(
                answer="I cannot fulfill this request. Please ask a specific question regarding scholarship requirements, deadlines, or benefits.",
                citations=[],
                confidenceScore=0.0,
                disclaimer="Input filtered by PrestIQ Prompt Injection Guard."
            )

        # 2. Guard Check: Deterministic Eligibility Boundary
        if self.check_eligibility_inquiry(question):
            logger.info("Redirected eligibility inquiry for scholarship %d to deterministic engine", scholarship_id)
            return ChatResponse(
                answer=(
                    "I can explain the requirements from the official guidelines, but official eligibility "
                    "assessments are calculated deterministically by PrestIQ's Eligibility Service to ensure accuracy. "
                    "Please visit the Eligibility tab to run your automated assessment."
                ),
                citations=[],
                confidenceScore=1.0,
                disclaimer="Official eligibility assessments are calculated deterministically by Prestiq's Eligibility Service."
            )

        # 3. Vector Retrieval & Similarity Filtering
        # Embed question
        try:
            query_embeddings = await self.factory.embed([question])
            query_vector = query_embeddings[0] if query_embeddings else []
        except Exception as e:
            logger.warning("Vector embedding failed (%s). Proceeding with fallback retrieval.", str(e))
            query_vector = []

        scored_chunks: List[Tuple[DocumentChunk, float]] = await self.vector_store.search(
            scholarship_id=scholarship_id,
            query_vector=query_vector,
            top_k=settings.rag_max_chunks,
            min_similarity=settings.rag_min_similarity
        )

        if not scored_chunks:
            # Check if scholarship even exists in knowledge store
            all_chunks = await self.vector_store.get_chunks_by_scholarship(scholarship_id)
            if not all_chunks:
                raise ScholarshipNotFoundError(scholarship_id)

            # Chunks exist but none matched question similarity threshold
            return ChatResponse(
                answer=(
                    "The indexed official guidelines for this scholarship do not contain sufficient verified evidence "
                    "to answer your question. Please consult the official scholarship website for verified details."
                ),
                citations=[],
                confidenceScore=0.0,
                disclaimer="No matching verified documentation found meeting similarity threshold (0.55)."
            )

        # 4. Assemble Grounded Prompt
        context_parts = []
        chunk_map = {}
        for idx, (chunk, score) in enumerate(scored_chunks, start=1):
            source_label = f"Source {idx}"
            chunk_map[idx] = chunk
            page_info = f", Page {chunk.page_number}" if chunk.page_number else ""
            context_parts.append(
                f"[{source_label}{page_info}] ({chunk.source_name or 'Official Guidelines'} - {chunk.source_url}):\n{chunk.content}\n"
            )

        context_text = "\n".join(context_parts)
        prompt = (
            f"Verified Documentation Excerpts:\n{context_text}\n\n"
            f"Student Question: <student_question>{question}</student_question>\n\n"
            f"Synthesize a grounded answer following all instructions and citing [Source X] for every fact:"
        )

        # 5. LLM Synthesis with Multi-Provider Fallback
        fallback_occurred = False
        provider_used = None
        try:
            response, fallback_occurred = await self.factory.generate_with_fallback(
                prompt=prompt,
                system_prompt=SYSTEM_PROMPT
            )
            raw_answer = response.content
            provider_used = response.provider
        except RateLimitExceededError:
            # Graceful Degradation: Direct excerpt return without LLM synthesis
            logger.info("All LLM providers rate-limited. Gracefully degrading to raw document excerpts.")
            raw_answer = (
                "Direct excerpts from official guidelines displayed (AI assistant in scheduled cooldown):\n\n"
                + "\n\n".join(f"• [Source {idx}]: {c.content}" for idx, (c, _) in enumerate(scored_chunks, 1))
            )
            fallback_occurred = True
            provider_used = "direct_excerpt_fallback"

        # 6. Citation Verification & Extraction
        citations = self._extract_citations(raw_answer, chunk_map)

        # 7. Confidence Score Calculation
        avg_similarity = sum(s for _, s in scored_chunks) / len(scored_chunks)
        confidence = min(0.98, max(0.60, avg_similarity))
        if not citations and provider_used != "direct_excerpt_fallback":
            confidence = max(0.50, confidence - 0.20)

        return ChatResponse(
            answer=raw_answer.strip(),
            citations=citations,
            confidenceScore=round(confidence, 2),
            disclaimer="Synthesized directly from verified provider documentation. Eligibility assessments are calculated deterministically by Prestiq's Eligibility Service.",
            providerUsed=provider_used,
            fallbackOccurred=fallback_occurred
        )

    def _extract_citations(
        self,
        answer: str,
        chunk_map: dict[int, DocumentChunk]
    ) -> List[Citation]:
        """Extract and structure verified citations from answer text."""
        citations: List[Citation] = []
        seen_indices = set()

        # Match [Source 1] or [Source 1, Page 2]
        matches = re.findall(r"\[Source\s*(\d+)(?:,\s*Page\s*(\d+))?\]", answer, re.IGNORECASE)

        for match in matches:
            idx = int(match[0])
            if idx in chunk_map and idx not in seen_indices:
                seen_indices.add(idx)
                chunk = chunk_map[idx]
                page_no = int(match[1]) if match[1] else chunk.page_number
                citations.append(
                    Citation(
                        sourceName=chunk.source_name or "Official Guidelines",
                        sourceUrl=chunk.source_url,
                        pageNumber=page_no,
                        verifiedExcerpt=chunk.content[:250] + ("..." if len(chunk.content) > 250 else "")
                    )
                )

        # If direct excerpt fallback was used, attach all top chunks as citations
        if not citations and chunk_map:
            for idx, chunk in sorted(chunk_map.items()):
                citations.append(
                    Citation(
                        sourceName=chunk.source_name or "Official Guidelines",
                        sourceUrl=chunk.source_url,
                        pageNumber=chunk.page_number,
                        verifiedExcerpt=chunk.content[:250] + ("..." if len(chunk.content) > 250 else "")
                    )
                )

        return citations
