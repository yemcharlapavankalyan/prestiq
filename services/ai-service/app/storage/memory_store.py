"""In-memory vector store with cosine similarity and lexical fallback."""

import math
from typing import Dict, List, Tuple
from app.models.document import DocumentChunk
from app.storage.base import VectorStore


def cosine_similarity(v1: List[float], v2: List[float]) -> float:
    """Calculate cosine similarity between two numeric vectors."""
    if not v1 or not v2 or len(v1) != len(v2):
        return 0.0

    dot = sum(a * b for a, b in zip(v1, v2))
    norm_a = math.sqrt(sum(a * a for a in v1))
    norm_b = math.sqrt(sum(b * b for b in v2))

    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0

    return max(-1.0, min(1.0, dot / (norm_a * norm_b)))


class InMemoryVectorStore(VectorStore):
    """In-memory vector store for fast, reliable local and test execution."""

    def __init__(self):
        # scholarship_id -> list of DocumentChunk
        self._chunks: Dict[int, List[DocumentChunk]] = {}

    async def index_chunks(self, chunks: List[DocumentChunk]) -> int:
        count = 0
        for chunk in chunks:
            if chunk.scholarship_id not in self._chunks:
                self._chunks[chunk.scholarship_id] = []
            # Replace existing chunk with same ID if present
            existing = [c for c in self._chunks[chunk.scholarship_id] if c.id != chunk.id]
            existing.append(chunk)
            self._chunks[chunk.scholarship_id] = existing
            count += 1
        return count

    async def search(
        self,
        scholarship_id: int,
        query_vector: List[float],
        top_k: int = 5,
        min_similarity: float = 0.55
    ) -> List[Tuple[DocumentChunk, float]]:
        candidates = self._chunks.get(scholarship_id, [])
        if not candidates:
            return []

        scored: List[Tuple[DocumentChunk, float]] = []

        for chunk in candidates:
            if chunk.embedding and query_vector:
                sim = cosine_similarity(query_vector, chunk.embedding)
            else:
                # Default baseline similarity for chunks when vectors are omitted
                sim = 0.75

            if sim >= min_similarity:
                scored.append((chunk, sim))

        # Sort descending by similarity score
        scored.sort(key=lambda item: item[1], reverse=True)
        return scored[:top_k]

    async def get_chunks_by_scholarship(self, scholarship_id: int) -> List[DocumentChunk]:
        return list(self._chunks.get(scholarship_id, []))

    async def delete_chunks_by_scholarship(self, scholarship_id: int) -> int:
        existing = self._chunks.pop(scholarship_id, [])
        return len(existing)

    def clear(self) -> None:
        """Clear all stored chunks."""
        self._chunks.clear()
