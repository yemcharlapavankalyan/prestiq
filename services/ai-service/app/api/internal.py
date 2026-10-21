"""Internal API endpoints for verified scholarship indexing and knowledge store management."""

from typing import Any, Dict, List
from fastapi import APIRouter, Depends, status
from app.api.deps import get_vector_store, require_internal_service
from app.core.security import UserPrincipal
from app.models.document import DocumentChunk
from app.models.schemas import IndexScholarshipRequest, IndexScholarshipResponse
from app.providers.factory import provider_factory
from app.services.chunker import chunk_text
from app.storage.base import VectorStore

router = APIRouter(prefix="/internal/v1", tags=["Internal Indexing API"])


@router.post(
    "/index/scholarship",
    response_model=IndexScholarshipResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Index verified scholarship documentation",
    description="Extracts, chunks, embeds, and indexes verified guidelines into vector storage."
)
async def index_scholarship(
    request: IndexScholarshipRequest,
    internal_user: UserPrincipal = Depends(require_internal_service),
    vector_store: VectorStore = Depends(get_vector_store)
) -> IndexScholarshipResponse:
    """Index sources for a verified scholarship opportunity."""
    all_chunks: List[DocumentChunk] = []

    for src in request.sources:
        content = src.content or ""
        if not content.strip():
            continue

        chunks = chunk_text(
            text=content,
            scholarship_id=request.scholarshipId,
            source_id=src.sourceId,
            source_url=src.sourceUrl,
            source_name=src.sourceName or "Official Guidelines",
            page_number=1
        )
        all_chunks.extend(chunks)

    if not all_chunks:
        return IndexScholarshipResponse(
            scholarshipId=request.scholarshipId,
            chunksIndexed=0,
            status="EMPTY_CONTENT"
        )

    # Generate embeddings in batch
    texts = [c.content for c in all_chunks]
    try:
        embeddings = await provider_factory.embed(texts)
        for chunk, emb in zip(all_chunks, embeddings):
            chunk.embedding = emb
    except Exception as e:
        # Proceed even if embedding fails: chunks will use lexical / baseline similarity
        pass

    indexed_count = await vector_store.index_chunks(all_chunks)

    return IndexScholarshipResponse(
        scholarshipId=request.scholarshipId,
        chunksIndexed=indexed_count,
        status="INDEXED"
    )


@router.get(
    "/chunks/{scholarship_id}",
    response_model=List[Dict[str, Any]],
    status_code=status.HTTP_200_OK,
    summary="Get indexed chunks for a scholarship",
    description="Internal inspection endpoint to view indexed chunks."
)
async def get_scholarship_chunks(
    scholarship_id: int,
    internal_user: UserPrincipal = Depends(require_internal_service),
    vector_store: VectorStore = Depends(get_vector_store)
) -> List[Dict[str, Any]]:
    """Retrieve indexed chunks for given scholarship ID."""
    chunks = await vector_store.get_chunks_by_scholarship(scholarship_id)
    return [
        {
            "id": str(c.id),
            "scholarshipId": c.scholarship_id,
            "sourceId": c.source_id,
            "chunkIndex": c.chunk_index,
            "sourceUrl": c.source_url,
            "sourceName": c.source_name,
            "pageNumber": c.page_number,
            "content": c.content,
            "tokenCount": c.token_count,
            "hasEmbedding": c.embedding is not None
        }
        for c in chunks
    ]


@router.delete(
    "/chunks/{scholarship_id}",
    response_model=Dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Delete indexed chunks for a scholarship",
    description="Deletes all indexed chunks when a scholarship is unpublished or withdrawn."
)
async def delete_scholarship_chunks(
    scholarship_id: int,
    internal_user: UserPrincipal = Depends(require_internal_service),
    vector_store: VectorStore = Depends(get_vector_store)
) -> Dict[str, Any]:
    """Delete all indexed chunks for scholarship ID."""
    deleted = await vector_store.delete_chunks_by_scholarship(scholarship_id)
    return {"scholarshipId": scholarship_id, "deletedChunks": deleted}
