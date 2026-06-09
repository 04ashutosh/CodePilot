import logging
from typing import List
from sentence_transformers import SentenceTransformer
from app.config import get_settings

logger = logging.getLogger(__name__)

# Model singleton instance
_model: SentenceTransformer | None = None

def get_embedding_model() -> SentenceTransformer:
    """
    Returns a singleton instance of the SentenceTransformer model.
    Downloads and loads the model on the first invocation.
    """
    global _model
    if _model is None:
        settings = get_settings()
        logger.info("Loading sentence-transformers model: %s", settings.embedding_model)
        _model = SentenceTransformer(settings.embedding_model)
        logger.info("Model loaded successfully")
    return _model

def generate_embeddings(texts: List[str]) -> List[List[float]]:
    """
    Generates embedding vectors for a list of text strings.
    Each vector has a dimension of 384 (for all-MiniLM-L6-v2).
    """
    if not texts:
        return []
    model = get_embedding_model()
    # encode returns numpy arrays; convert to list of floats for JSON/Chroma serialization
    embeddings = model.encode(texts, convert_to_numpy=True)
    return embeddings.tolist()

def generate_embedding(text: str) -> List[float]:
    """
    Generates embedding vector for a single text string.
    """
    return generate_embeddings([text])[0]