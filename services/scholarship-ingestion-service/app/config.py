"""PrestIQ Scholarship Ingestion Service Configuration."""

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
    service_name: str = "scholarship-ingestion-service"
    service_version: str = "1.0.0"
    port: int = 8001
    env: str = "development"
    debug: bool = False

    # Security
    jwt_secret: str = "d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2"
    jwt_issuer: str = "prestiq-auth"
    jwt_algorithm: str = "HS256"
    internal_service_key: str = "prestiq-internal-dev-secret-key-2026"
    cors_allowed_origins: str = "http://localhost:3000,http://localhost:5173,http://localhost:8080"

    # Database: SQLite in-memory/file for lightweight testing or PostgreSQL in production
    database_url: str = "sqlite+aiosqlite:///./prestiq_ingestion.db"

    # Inter-Service URLs
    scholarship_service_url: str = "http://localhost:8083"
    ai_service_url: str = "http://localhost:8000"

    # Polite Crawler & Network Configuration
    user_agent: str = "PrestiqScholarshipBot/1.0 (+https://prestiq.org/bot)"
    max_response_bytes: int = 5 * 1024 * 1024  # 5 MB
    connect_timeout_seconds: float = 5.0
    read_timeout_seconds: float = 15.0
    max_retries: int = 3
    retry_backoff_base: float = 1.5
    rate_limit_default_rpm: int = 15
    max_concurrent_runs: int = 2

    # LLM-Assisted Extraction Configuration
    llm_extraction_enabled: bool = True
    llm_provider: str = "mock"  # "mock", "gemini", "groq", "cerebras"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-1.5-flash"
    groq_api_key: str = ""
    groq_model: str = "llama-3.3-70b-versatile"
    cerebras_api_key: str = ""
    cerebras_model: str = "llama-3.1-70b"

    # Scheduler Configuration
    scheduler_enabled: bool = False
    scheduler_check_interval_seconds: int = 60


settings = Settings()
