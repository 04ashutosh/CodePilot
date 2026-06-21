from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "Execution Sandbox Service"
    docker_timeout: int = 30          # seconds
    max_memory: str = "128m"          # Docker memory limit
    max_cpus: float = 0.5             # Docker CPU limit
    allowed_languages: list = ["python", "javascript","java"]

    class Config:
        env_file = ".env"

settings = Settings()