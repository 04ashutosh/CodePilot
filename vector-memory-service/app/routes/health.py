"""
Health check endpoint for the Vector Memory Service.

Used by:
- Docker HEALTHCHECK to determine container liveness.
- API Gateway to verify the service is reachable.
- Kubernetes readiness/liveness probes (future).

Returns the status of the service AND its ChromaDB dependency.
"""

from fastapi import APIRouter
from app.config import get_settings
from app.services.chroma_client import check_health as chroma_health

router = APIRouter()


@router.get("/health")
async def health_check():
    """
    GET /health — Service health check.

    Returns 200 with service info and ChromaDB connection status.
    Even if ChromaDB is down, the service itself is "up" —
    callers should check the chroma.status field.
    """
    settings = get_settings()
    chroma_status = chroma_health()

    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "status": "up",
        "dependencies": {
            "chromadb": chroma_status,
        },
    }