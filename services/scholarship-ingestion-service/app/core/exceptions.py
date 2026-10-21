"""RFC 7807 Problem Details and Domain Exceptions for Ingestion Service."""

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


class IngestionServiceException(Exception):
    """Base exception for all domain errors."""
    def __init__(
        self,
        detail: str,
        status_code: int = 500,
        title: str = "Internal Server Error",
        error_code: str = "ERR_INTERNAL",
        invalid_params: Optional[List[Dict[str, Any]]] = None,
    ):
        super().__init__(detail)
        self.detail = detail
        self.status_code = status_code
        self.title = title
        self.error_code = error_code
        self.invalid_params = invalid_params or []


class SSRFSecurityError(IngestionServiceException):
    """Raised when an outbound URL violates SSRF or allowed-domain restrictions."""
    def __init__(self, detail: str):
        super().__init__(
            detail=detail,
            status_code=400,
            title="SSRF Security Violation",
            error_code="ERR_SSRF_PROHIBITED",
        )


class SourceNotFoundError(IngestionServiceException):
    """Raised when requested source does not exist."""
    def __init__(self, source_id: str):
        super().__init__(
            detail=f"Source with id '{source_id}' was not found.",
            status_code=404,
            title="Source Not Found",
            error_code="ERR_SOURCE_NOT_FOUND",
        )


class RunNotFoundError(IngestionServiceException):
    """Raised when requested ingestion run does not exist."""
    def __init__(self, run_id: str):
        super().__init__(
            detail=f"Ingestion run with id '{run_id}' was not found.",
            status_code=404,
            title="Run Not Found",
            error_code="ERR_RUN_NOT_FOUND",
        )


class CandidateNotFoundError(IngestionServiceException):
    """Raised when requested candidate scholarship does not exist."""
    def __init__(self, candidate_id: str):
        super().__init__(
            detail=f"Candidate with id '{candidate_id}' was not found.",
            status_code=404,
            title="Candidate Not Found",
            error_code="ERR_CANDIDATE_NOT_FOUND",
        )


class SourceConflictError(IngestionServiceException):
    """Raised when attempting to create a source with an existing identifier."""
    def __init__(self, source_id: str):
        super().__init__(
            detail=f"Source with id '{source_id}' already exists.",
            status_code=409,
            title="Source Conflict",
            error_code="ERR_SOURCE_CONFLICT",
        )


class SourceExecutionLockedError(IngestionServiceException):
    """Raised when a run is already active for this source."""
    def __init__(self, source_id: str):
        super().__init__(
            detail=f"An ingestion job is currently executing for source '{source_id}'. Overlapping runs are prohibited.",
            status_code=409,
            title="Run Overlap Prohibited",
            error_code="ERR_SOURCE_RUN_LOCKED",
        )


class UpstreamFetchError(IngestionServiceException):
    """Raised when an external source fetch fails."""
    def __init__(self, detail: str, status_code: int = 502):
        super().__init__(
            detail=detail,
            status_code=status_code,
            title="Upstream Fetch Error",
            error_code="ERR_UPSTREAM_FETCH",
        )


class ResponseTooLargeError(IngestionServiceException):
    """Raised when an upstream document exceeds max allowable size."""
    def __init__(self, size_bytes: int, max_bytes: int):
        super().__init__(
            detail=f"Downloaded response size {size_bytes} exceeds limit of {max_bytes} bytes.",
            status_code=413,
            title="Payload Too Large",
            error_code="ERR_RESPONSE_TOO_LARGE",
        )


def make_problem_detail(
    status: int,
    title: str,
    detail: str,
    instance: str,
    code: str,
    invalid_params: Optional[List[Dict[str, Any]]] = None,
) -> Dict[str, Any]:
    """Formats standard RFC 7807 problem details JSON."""
    prob: Dict[str, Any] = {
        "type": f"https://api.prestiq.org/errors/{code.lower().replace('_', '-')}",
        "title": title,
        "status": status,
        "detail": detail,
        "instance": instance,
        "code": code,
        "serviceId": "scholarship-ingestion-service",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
    if invalid_params:
        prob["invalidParams"] = invalid_params
    return prob


def register_exception_handlers(app: FastAPI) -> None:
    """Registers global exception handlers for RFC 7807 responses."""

    @app.exception_handler(IngestionServiceException)
    async def domain_exception_handler(request: Request, exc: IngestionServiceException):
        payload = make_problem_detail(
            status=exc.status_code,
            title=exc.title,
            detail=exc.detail,
            instance=str(request.url.path),
            code=exc.error_code,
            invalid_params=exc.invalid_params,
        )
        return JSONResponse(status_code=exc.status_code, content=payload)

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        invalid_params = []
        for error in exc.errors():
            loc = ".".join(str(item) for item in error.get("loc", []))
            invalid_params.append({
                "field": loc,
                "message": error.get("msg", "Invalid parameter"),
                "type": error.get("type", "value_error"),
            })
        payload = make_problem_detail(
            status=422,
            title="Validation Failed",
            detail="One or more request parameters failed validation criteria.",
            instance=str(request.url.path),
            code="ERR_VALIDATION_FAILED",
            invalid_params=invalid_params,
        )
        return JSONResponse(status_code=422, content=payload)

    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        payload = make_problem_detail(
            status=exc.status_code,
            title="HTTP Error",
            detail=str(exc.detail),
            instance=str(request.url.path),
            code=f"ERR_HTTP_{exc.status_code}",
        )
        return JSONResponse(status_code=exc.status_code, content=payload)

    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception):
        payload = make_problem_detail(
            status=500,
            title="Internal Server Error",
            detail="An unexpected server error occurred.",
            instance=str(request.url.path),
            code="ERR_INTERNAL_ERROR",
        )
        return JSONResponse(status_code=500, content=payload)
