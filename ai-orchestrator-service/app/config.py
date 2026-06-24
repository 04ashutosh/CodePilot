from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "AI Orchestrator Service"
    vector_memory_url: str = "http://localhost:8091" # Local test URL
    repository_service_url: str = "http://localhost:8083" # Local test URL
    project_service_url: str = "http://localhost:8082" # Local test URL
    ollama_base_url: str = "http://localhost:11434" # Ollama default
    execution_service_url: str = "http://localhost:8084"
    kafka_bootstrap_servers: str = "localhost:9092"
    
    class Config:
        env_file = ".env"

settings = Settings()