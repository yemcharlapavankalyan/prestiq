"""Integration tests for internal indexing endpoints."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_index_and_retrieve_scholarship_chunks(client: AsyncClient, internal_key: str):
    payload = {
        "scholarshipId": 201,
        "sources": [
            {
                "sourceId": 1,
                "sourceUrl": "https://example.org/guidelines.pdf",
                "sourceName": "2026 Guidelines",
                "contentType": "text/plain",
                "content": "This is official scholarship documentation. Eligible applicants must be enrolled in STEM degrees. The maximum award amount is $10,000."
            }
        ]
    }

    # 1. Index document
    index_resp = await client.post(
        "/internal/v1/index/scholarship",
        headers={"X-Internal-Service-Key": internal_key},
        json=payload
    )
    assert index_resp.status_code == 202
    data = index_resp.json()
    assert data["scholarshipId"] == 201
    assert data["chunksIndexed"] >= 1
    assert data["status"] == "INDEXED"

    # 2. Retrieve chunks
    chunks_resp = await client.get(
        "/internal/v1/chunks/201",
        headers={"X-Internal-Service-Key": internal_key}
    )
    assert chunks_resp.status_code == 200
    chunks = chunks_resp.json()
    assert len(chunks) >= 1
    assert chunks[0]["scholarshipId"] == 201
    assert "STEM degrees" in chunks[0]["content"]

    # 3. Delete chunks
    del_resp = await client.delete(
        "/internal/v1/chunks/201",
        headers={"X-Internal-Service-Key": internal_key}
    )
    assert del_resp.status_code == 200
    assert del_resp.json()["deletedChunks"] >= 1

    # 4. Verify chunks deleted
    verify_resp = await client.get(
        "/internal/v1/chunks/201",
        headers={"X-Internal-Service-Key": internal_key}
    )
    assert len(verify_resp.json()) == 0
