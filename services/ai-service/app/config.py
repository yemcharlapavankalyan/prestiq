"""PrestIQ AI Service Configuration."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables or defaults."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Service
    service_name: str = "ai-service"
    service_version: str = "1.0.0"
    port: int = 8000
    env: str = "development"
    debug: bool = False

    # Security
    jwt_secret: str = "0123456789012345678901234567890123456789012345678901234567890123"
    jwt_issuer: str = "prestiq-auth-service"
    jwt_algorithm: str = "HS256"
    internal_service_key: str = "prestiq-internal-dev-secret-key-2026"

    # Multi-Provider Routing
    primary_ai_provider: str = "gemini"
    fallback_ai_provider: str = "groq"
    tertiary_ai_provider: str = "cerebras"
    emergency_ai_provider: str = "openrouter"
    allow_paid_fallback: bool = False
    use_mock_providers: bool = False

    # Google Gemini
    gemini_api_key: str = ""
    gemini_model: str = "gemini-1.5-flash"
    gemini_embedding_model: str = "text-embedding-004"
    gemini_rpm_limit: int = 15
    gemini_base_url: str = "https://generativelanguage.googleapis.com/v1beta"

    # Groq Cloud
    groq_api_key: str = ""
    groq_model: str = "llama-3.3-70b-versatile"
    groq_fast_model: str = "llama-3.1-8b-instant"
    groq_rpm_limit: int = 30
    groq_base_url: str = "https://api.groq.com/openai/v1"

    # Cerebras
    cerebras_api_key: str = ""
    cerebras_model: str = "llama-3.1-70b"
    cerebras_rpm_limit: int = 30
    cerebras_base_url: str = "https://api.cerebras.ai/v1"

    # OpenRouter
    openrouter_api_key: str = ""
    openrouter_model: str = "meta-llama/llama-3.1-8b-instruct:free"
    openrouter_rpm_limit: int = 20
    openrouter_base_url: str = "https://openrouter.ai/api/v1"

    # Vector Retrieval
    embedding_dimensions: int = 768
    rag_min_similarity: float = 0.55
    rag_max_chunks: int = 5

    # Database
    ai_db_url: str = "postgresql://prestiq_user:prestiq_password@localhost:5432/prestiq_ai_db"

    # Timeouts & Request Limits
    http_timeout_seconds: float = 8.0
    client_rate_limit_rpm: int = 30
    max_question_length: int = 400


settings = Settings()
