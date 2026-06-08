from pydantic_settings import BaseSettings
from functools import lru_cache
class Settings(BaseSettings):
    """Central configuration for the Vector Memory Service."""
    # ===== Service Identity =====
    app_name: str = "vector-memory-service"
    app_version: str = "1.0.0"
    debug: bool = False
    # ===== Server =====
    host: str = "0.0.0.0"
    port: int = 8091
    # ===== ChromaDB Connection =====
    # Points to the ChromaDB container defined in docker-compose.yml.
    chroma_host: str = "localhost"
    chroma_port: int = 8000
    # ===== Embedding Model =====
    # all-MiniLM-L6-v2: 384-dimensional vectors, fast, good quality.
    # Downloaded automatically on first run by sentence-transformers.
    embedding_model: str = "all-MiniLM-L6-v2"
    embedding_dimension: int = 384
    # ===== Code Chunking =====
    # Max lines per chunk when splitting source files.
    chunk_max_lines: int = 50
    # Overlap between consecutive chunks (in lines) for context continuity.
    chunk_overlap_lines: int = 10
    # ===== JWT (shared secret with Java services) =====
    jwt_secret: str = "Y29kZXBpbG90LWFpLXN1cGVyLXNlY3JldC1rZXktMjAyNi1wcm9kdWN0aW9uLWdyYWRl"
    # ===== Internal API Key (service-to-service auth) =====
    internal_api_key: str = "codepilot-internal-key-2026"
    # ===== Repository Service URL (for fetching file contents) =====
    repository_service_url: str = "http://localhost:8083"
    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
        "case_sensitive": False,
    }
@lru_cache()
def get_settings() -> Settings:
    """
    Returns a cached singleton Settings instance.
    lru_cache ensures the .env file is read only once.
    """
    return Settings()