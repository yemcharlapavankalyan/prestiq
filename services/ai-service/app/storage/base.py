"""Abstract base class for vector storage and retrieval."""

from abc import ABC, abstractmethod
from typing import List, Tuple
from app.models.document import DocumentChunk


class VectorStore(ABC):
    """Abstract vector storage interface for document chunks."""

    @abstractmethod
    async def index_chunks(self, chunks: List[DocumentChunk]) -> int:
        """Store and index document chunks. Returns number of chunks stored."""
        pass

    @abstractmethod
    async def search(
        self,
        scholarship_id: int,
        query_vector: List[float],
        top_k: int = 5,
        min_similarity: float = 0.55
    ) -> List[Tuple[DocumentChunk, float]]:
        """
        Search for top matching chunks for a scholarship by cosine similarity.
        Returns list of (DocumentChunk, similarity_score).
        """
        pass

    @abstractmethod
    async def get_chunks_by_scholarship(self, scholarship_id: int) -> List[DocumentChunk]:
        """Retrieve all indexed chunks for a given scholarship ID."""
        pass

    @abstractmethod
    async def delete_chunks_by_scholarship(self, scholarship_id: int) -> int:
        """Delete all chunks for a given scholarship. Returns number deleted."""
        pass
