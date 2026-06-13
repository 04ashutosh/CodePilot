import logging
from langchain_ollama import ChatOllama
from app.config import settings

logger = logging.getLogger(__name__)

def get_ollama_llm(model_name: str = "qwen2.5-coder:latest", temperature: float = 0.1):
    """
    Initializes and returns the LangChain wrapper for Ollama.
    Uses qwen2.5-coder by default for high-quality code generation.
    """
    try:
        logger.info(f"Initializing Ollama client with model: {model_name} at {settings.ollama_base_url}")
        llm = ChatOllama(
            base_url=settings.ollama_base_url,
            model=model_name,
            temperature=temperature
        )
        return llm
    except Exception as e:
        logger.error(f"Failed to initialize Ollama LLM: {e}")
        raise e