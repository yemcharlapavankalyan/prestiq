"""Public Assistant Chat API for source-grounded scholarship Q&A."""

from fastapi import APIRouter, Depends, status
from app.api.deps import get_current_user, get_qa_engine
from app.core.security import UserPrincipal
from app.models.schemas import ChatRequest, ChatResponse
from app.services.qa_engine import QAEngine

router = APIRouter(prefix="/api/v1/assistant", tags=["Assistant API"])


@router.post(
    "/chat",
    response_model=ChatResponse,
    status_code=status.HTTP_200_OK,
    summary="Ask a source-grounded question about a verified scholarship",
    description="Retrieves official documentation chunks and synthesizes an answer with verified citations."
)
async def chat(
    request: ChatRequest,
    current_user: UserPrincipal = Depends(get_current_user),
    qa_engine: QAEngine = Depends(get_qa_engine)
) -> ChatResponse:
    """Execute grounded Q&A with mandatory citations and anti-hallucination guardrails."""
    return await qa_engine.answer_question(
        scholarship_id=request.scholarshipId,
        question=request.question
    )
