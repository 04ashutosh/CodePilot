import logging
import chromadb
from chromadb.config import Settings as ChromaSettings
from app.config import get_settings
logger = logging.getLogger(__name__)
# Module-level client — initialized once on first import.
_client: chromadb.HttpClient | None = None
def get_chroma_client() -> chromadb.HttpClient:
    """
    Returns a singleton ChromaDB HTTP client.
    Connects to the ChromaDB server defined in config.
    """
    global _client
    if _client is None:
        settings = get_settings()
        logger.info(
            "Connecting to ChromaDB at %s:%s",
            settings.chroma_host,
            settings.chroma_port,
        )
        _client = chromadb.HttpClient(
            host=settings.chroma_host,
            port=settings.chroma_port,
            settings=ChromaSettings(
                anonymized_telemetry=False,  # Disable telemetry in production
            ),
        )
        logger.info("ChromaDB client initialized successfully")
    return _client
def get_or_create_collection(repository_id: str) -> chromadb.Collection:
    """
    Gets or creates a ChromaDB collection for a specific repository.
    Collection name format: "repo_{repositoryId}"
    Each collection stores:
    - ids: unique chunk identifiers (repo_id + file_path + chunk_index)
    - embeddings: 384-dim vectors from sentence-transformers
    - documents: the raw code chunk text
    - metadatas: file_path, language, line_start, line_end, repo_id
    """
    client = get_chroma_client()
    collection_name = f"repo_{repository_id}"
    # ChromaDB collection names must be 3-63 chars, alphanumeric + underscores.
    # Our format "repo_{mongoId}" is always valid (mongo IDs are 24 hex chars).
    collection = client.get_or_create_collection(
        name=collection_name,
        metadata={"repository_id": repository_id},
    )
    logger.debug("Collection '%s' ready (%d items)", collection_name, collection.count())
    return collection
def delete_collection(repository_id: str) -> bool:
    """
    Deletes the entire collection for a repository.
    Used before re-indexing to ensure clean state.
    Returns True if deleted, False if collection didn't exist.
    """
    client = get_chroma_client()
    collection_name = f"repo_{repository_id}"
    try:
        client.delete_collection(name=collection_name)
        logger.info("Deleted collection '%s'", collection_name)
        return True
    except Exception:
        logger.debug("Collection '%s' not found, nothing to delete", collection_name)
        return False
def check_health() -> dict:
    """
    Checks ChromaDB connectivity and returns status info.
    Used by the /health endpoint.
    """
    try:
        client = get_chroma_client()
        heartbeat = client.heartbeat()
        collections = client.list_collections()
        return {
            "status": "healthy",
            "heartbeat": heartbeat,
            "total_collections": len(collections),
        }
    except Exception as e:
        logger.error("ChromaDB health check failed: %s", str(e))
        return {
            "status": "unhealthy",
            "error": str(e),
        }