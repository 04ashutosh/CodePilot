import logging
from fastapi import APIRouter, HTTPException, status
from app.models.schemas import ExecutionRequest, ExecutionResponse
from app.services.docker_executor import execute_code

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/run", response_model=ExecutionResponse)
async def run_code(request: ExecutionRequest):
    """
    POST /api/exec/run
    Executes code in an isolated Docker sandbox and returns the result.
    """
    logger.info(f"Execution request received — language: {request.language}, timeout: {request.timeout}s")

    try:
        result = execute_code(
            code=request.code,
            language=request.language,
            timeout=request.timeout
        )
        return ExecutionResponse(**result)

    except Exception as e:
        logger.error(f"Execution endpoint error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to execute code: {str(e)}"
        )