"""Custom domain exceptions and RFC 7807 Problem Details handlers."""

from typing import Optional
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


class PrestIQAIException(Exception):
    """Base exception for AI Service domain errors."""

    def __init__(self, message: str, status_code: int = 400, error_code: str = "ERR_BAD_REQUEST"):
        super().__init__(message)
        self.message = message
        self.status_code = status_code
        self.error_code = error_code


class AuthenticationError(PrestIQAIException):
    def __init__(self, message: str = "Authentication required"):
        super().__init__(message, status_code=status.HTTP_401_UNAUTHORIZED, error_code="ERR_UNAUTHORIZED")


class AuthorizationError(PrestIQAIException):
    def __init__(self, message: str = "Access denied"):
        super().__init__(message, status_code=status.HTTP_403_FORBIDDEN, error_code="ERR_FORBIDDEN")


class ScholarshipNotFoundError(PrestIQAIException):
    def __init__(self, scholarship_id: int):
        super().__init__(
            f"No verified documentation indexed for scholarship ID {scholarship_id}",
            status_code=status.HTTP_404_NOT_FOUND,
            error_code="ERR_SCHOLARSHIP_NOT_FOUND"
        )


class RateLimitExceededError(PrestIQAIException):
    def __init__(self, message: str = "Rate limit exceeded. Please retry after a brief cooldown."):
        super().__init__(message, status_code=status.HTTP_429_TOO_MANY_REQUESTS, error_code="ERR_RATE_LIMITED")


class ProviderUnavailableError(PrestIQAIException):
    def __init__(self, message: str = "AI providers temporarily unavailable"):
        super().__init__(message, status_code=status.HTTP_503_SERVICE_UNAVAILABLE, error_code="ERR_PROVIDER_UNAVAILABLE")


def create_problem_detail(
    request: Request,
    title: str,
    status_code: int,
    detail: str,
    error_code: str,
    invalid_params: Optional[dict] = None
) -> JSONResponse:
    """Construct RFC 7807 compliant problem details response."""
    payload = {
        "type": f"https://api.prestiq.org/errors/{error_code.lower().replace('_', '-')}",
        "title": title,
        "status": status_code,
        "detail": detail,
        "instance": str(request.url.path),
        "code": error_code
    }
    if invalid_params:
        payload["invalidParams"] = invalid_params

    return JSONResponse(
        status_code=status_code,
        content=payload,
        media_type="application/problem+json"
    )


def register_exception_handlers(app: FastAPI) -> None:
    """Register global exception handlers on FastAPI application."""

    @app.exception_handler(PrestIQAIException)
    async def handle_prestigq_ai_exception(request: Request, exc: PrestIQAIException):
        title = exc.error_code.replace("ERR_", "").replace("_", " ").title()
        return create_problem_detail(
            request=request,
            title=title,
            status_code=exc.status_code,
            detail=exc.message,
            error_code=exc.error_code
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(request: Request, exc: RequestValidationError):
        invalid_fields = {}
        for err in exc.errors():
            loc = ".".join(str(x) for x in err.get("loc", []) if x != "body")
            invalid_fields[loc] = err.get("msg", "Invalid value")

        return create_problem_detail(
            request=request,
            title="Validation Failed",
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="One or more request parameters failed validation criteria.",
            error_code="ERR_VALIDATION_FAILED",
            invalid_params=invalid_fields
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_exception(request: Request, exc: Exception):
        return create_problem_detail(
            request=request,
            title="Internal Server Error",
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected error occurred while processing the request.",
            error_code="ERR_INTERNAL_SERVER_ERROR"
        )
