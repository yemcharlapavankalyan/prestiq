"""FastAPI Dependencies for Ingestion Service."""

from typing import AsyncGenerator
from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.pipeline.orchestrator import IngestionOrchestrator
from app.storage.database import get_db_session
from app.storage.repository import IngestionRepository


async def get_repository(session: AsyncSession = Depends(get_db_session)) -> IngestionRepository:
    """Dependency for obtaining an IngestionRepository instance."""
    return IngestionRepository(session)


async def get_orchestrator(
    repo: IngestionRepository = Depends(get_repository),
) -> IngestionOrchestrator:
    """Dependency for obtaining an IngestionOrchestrator instance."""
    return IngestionOrchestrator(repository=repo)
