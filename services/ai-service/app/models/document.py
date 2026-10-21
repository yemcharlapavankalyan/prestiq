"""Document and chunk models for AI knowledge store."""

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional
from uuid import UUID, uuid4


@dataclass
class DocumentChunk:
    """Indexed document chunk for scholarship guideline retrieval."""

    scholarship_id: int
    content: str
    source_url: str
    source_id: int = 1
    chunk_index: int = 0
    token_count: int = 0
    heading_path: Optional[str] = None
    source_name: Optional[str] = None
    page_number: Optional[int] = None
    embedding: Optional[list[float]] = None
    id: UUID = field(default_factory=uuid4)
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    def __post_init__(self):
        if not self.token_count and self.content:
            # Approximate token count if not provided
            self.token_count = max(1, len(self.content.split()))
