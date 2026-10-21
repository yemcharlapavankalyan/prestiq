"""Document chunking and text segmentation utilities."""

from typing import List, Optional
from app.models.document import DocumentChunk


def chunk_text(
    text: str,
    scholarship_id: int,
    source_id: int = 1,
    source_url: str = "",
    source_name: Optional[str] = None,
    heading_path: Optional[str] = None,
    page_number: Optional[int] = 1,
    chunk_size_words: int = 200,
    overlap_words: int = 30
) -> List[DocumentChunk]:
    """
    Split document text into overlapping chunks preserving sentence/word boundaries.
    """
    if not text or not text.strip():
        return []

    words = text.split()
    if len(words) <= chunk_size_words:
        return [
            DocumentChunk(
                scholarship_id=scholarship_id,
                source_id=source_id,
                chunk_index=0,
                content=text.strip(),
                token_count=len(words),
                heading_path=heading_path,
                source_url=source_url,
                source_name=source_name,
                page_number=page_number
            )
        ]

    chunks: List[DocumentChunk] = []
    start = 0
    chunk_index = 0

    while start < len(words):
        end = min(start + chunk_size_words, len(words))
        chunk_words = words[start:end]
        chunk_content = " ".join(chunk_words)

        chunks.append(
            DocumentChunk(
                scholarship_id=scholarship_id,
                source_id=source_id,
                chunk_index=chunk_index,
                content=chunk_content,
                token_count=len(chunk_words),
                heading_path=heading_path,
                source_url=source_url,
                source_name=source_name,
                page_number=page_number
            )
        )
        chunk_index += 1

        if end >= len(words):
            break
        start += (chunk_size_words - overlap_words)

    return chunks
