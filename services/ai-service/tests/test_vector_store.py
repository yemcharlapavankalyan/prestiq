"""Tests for vector storage, cosine similarity, and chunk filtering."""

import pytest
from app.models.document import DocumentChunk
from app.storage.memory_store import InMemoryVectorStore, cosine_similarity


def test_cosine_similarity_calculation():
    v1 = [1.0, 0.0, 0.0]
    v2 = [1.0, 0.0, 0.0]
    assert pytest.approx(cosine_similarity(v1, v2), 0.001) == 1.0

    v3 = [0.0, 1.0, 0.0]
    assert pytest.approx(cosine_similarity(v1, v3), 0.001) == 0.0

    v4 = [-1.0, 0.0, 0.0]
    assert pytest.approx(cosine_similarity(v1, v4), 0.001) == -1.0


@pytest.mark.asyncio
async def test_in_memory_vector_store_crud_and_search():
    store = InMemoryVectorStore()

    c1 = DocumentChunk(
        scholarship_id=101,
        content="Eligibility requirement: minimum GPA 3.5",
        source_url="https://example.org/guidelines.pdf",
        source_name="Guidelines",
        page_number=1,
        embedding=[1.0, 0.0, 0.0]
    )
    c2 = DocumentChunk(
        scholarship_id=101,
        content="Application deadline is December 1, 2026",
        source_url="https://example.org/guidelines.pdf",
        source_name="Guidelines",
        page_number=2,
        embedding=[0.0, 1.0, 0.0]
    )
    c_other = DocumentChunk(
        scholarship_id=202,
        content="Unrelated scholarship guidelines",
        source_url="https://other.org/rules.pdf",
        embedding=[1.0, 0.0, 0.0]
    )

    indexed = await store.index_chunks([c1, c2, c_other])
    assert indexed == 3

    # Query targeting c1
    query_vec = [1.0, 0.0, 0.0]
    results = await store.search(scholarship_id=101, query_vector=query_vec, top_k=5, min_similarity=0.55)
    assert len(results) == 1
    assert results[0][0].id == c1.id
    assert pytest.approx(results[0][1], 0.01) == 1.0

    # Query for scholarship 202
    results_other = await store.search(scholarship_id=202, query_vector=query_vec)
    assert len(results_other) == 1
    assert results_other[0][0].id == c_other.id

    # Retrieve all chunks
    all_101 = await store.get_chunks_by_scholarship(101)
    assert len(all_101) == 2

    # Delete chunks
    deleted = await store.delete_chunks_by_scholarship(101)
    assert deleted == 2
    assert len(await store.get_chunks_by_scholarship(101)) == 0
