"""Integration tests for POST /api/v1/assistant/chat endpoint."""

import pytest
from httpx import AsyncClient
import app.api.deps as deps
from app.models.document import DocumentChunk


@pytest.mark.asyncio
async def test_chat_successful_grounded_answer(client: AsyncClient, student_token: str):
    # Pre-index a chunk into the active vector store
    store = deps.get_vector_store()
    emb = await deps.provider_factory.embed(["Does this scholarship cover graduate students?"])
    chunk = DocumentChunk(
        scholarship_id=101,
        content="Applicants must be undergraduate students enrolled full-time with a minimum 3.5 GPA.",
        source_url="https://stemfoundation.org/guidelines.pdf",
        source_name="2026 Eligibility Guidelines",
        page_number=2,
        embedding=emb[0]
    )
    await store.index_chunks([chunk])

    payload = {
        "scholarshipId": 101,
        "question": "Does this scholarship cover graduate students?"
    }

    response = await client.post(
        "/api/v1/assistant/chat",
        headers={"Authorization": f"Bearer {student_token}"},
        json=payload
    )

    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
    assert "citations" in data
    assert data["confidenceScore"] >= 0.5
    assert "Eligibility Service" in data["disclaimer"]


@pytest.mark.asyncio
async def test_chat_validation_errors(client: AsyncClient, student_token: str):
    # 1. Missing question
    resp1 = await client.post(
        "/api/v1/assistant/chat",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"scholarshipId": 101}
    )
    assert resp1.status_code == 422
    assert resp1.json()["code"] == "ERR_VALIDATION_FAILED"

    # 2. Question exceeds 400 characters
    resp2 = await client.post(
        "/api/v1/assistant/chat",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"scholarshipId": 101, "question": "A" * 401}
    )
    assert resp2.status_code == 422


@pytest.mark.asyncio
async def test_chat_scholarship_not_found_returns_404(client: AsyncClient, student_token: str):
    response = await client.post(
        "/api/v1/assistant/chat",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"scholarshipId": 9999, "question": "What is the award amount?"}
    )
    assert response.status_code == 404
    data = response.json()
    assert data["code"] == "ERR_SCHOLARSHIP_NOT_FOUND"
    assert "9999" in data["detail"]
