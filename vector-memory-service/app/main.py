"""
Vector Memory Service — FastAPI Application Entry Point.
This service provides semantic code search capabilities by:
1. Splitting source code into meaningful chunks.
2. Generating vector embeddings using sentence-transformers.
3. Storing embeddings in ChromaDB for fast similarity search.
4. Exposing a search API for the AI Orchestrator to find relevant code.
Runs on port 8091 by default.
"""
import logging
import sys
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import get_settings
from app.routes import health
# ===== Logging Configuration =====
# JSON-structured logging for production observability.
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s [%(name)s] %(levelname)s — %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager.
    - Startup: Log config, verify ChromaDB connection.
    - Shutdown: Clean up resources.
    """
    settings = get_settings()
    logger.info("Starting %s v%s", settings.app_name, settings.app_version)
    logger.info("ChromaDB: %s:%s", settings.chroma_host, settings.chroma_port)
    logger.info("Embedding model: %s (%d dims)", settings.embedding_model, settings.embedding_dimension)
    # Attempt to connect to ChromaDB on startup (non-fatal if it fails).
    try:
        from app.services.chroma_client import get_chroma_client
        client = get_chroma_client()
        heartbeat = client.heartbeat()
        logger.info("ChromaDB connected successfully (heartbeat: %s)", heartbeat)
    except Exception as e:
        logger.warning("ChromaDB not available at startup: %s (will retry on first request)", str(e))
    yield  # Application is running
    # Shutdown cleanup
    logger.info("Shutting down %s", settings.app_name)
# ===== Create FastAPI App =====
settings = get_settings()
app = FastAPI(
    title="CodePilot Vector Memory Service",
    description="Semantic code search using vector embeddings and ChromaDB",
    version=settings.app_version,
    lifespan=lifespan,
)
# ===== CORS Middleware =====
# Allow the Angular frontend (port 4200) and gateway (port 8080) to call us.
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",   # Angular dev server
        "http://localhost:8080",   # API Gateway
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# ===== Register Routes =====
app.include_router(health.router, tags=["Health"])
# Placeholder for Phase 2.5 routes:
# app.include_router(indexing.router, prefix="/api/search", tags=["Indexing"])
# app.include_router(search.router, prefix="/api/search", tags=["Search"])
# ===== Direct Run Support =====
# Allows running with: python -m app.main
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
    )