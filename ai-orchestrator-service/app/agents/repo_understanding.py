import logging
import httpx
from typing import Dict, Any
from app.models.schemas import AgentState
from app.config import settings

logger = logging.getLogger(__name__)

async def fetch_repo_structure(repository_id: str) -> Dict[str, Any]:
    """
    Fetches the file tree from the Repository Service.
    """
    try:
        url = f"{settings.repository_service_url}/api/repos/{repository_id}/tree"
        async with httpx.AsyncClient() as client:
            response = await client.get(url, timeout=10.0)
            response.raise_for_status()
            return response.json()
    except Exception as e:
        logger.error(f"Failed to fetch repo structure for {repository_id}: {e}")
        return {}

async def repo_understanding_node(state: AgentState) -> Dict[str, Any]:
    """
    LangGraph node for fetching and analyzing repository structure.
    """
    logger.info(f"--- REPO UNDERSTANDING NODE [Task: {state['task_id']}] ---")
    
    repo_id = state.get("repository_id")
    file_tree = await fetch_repo_structure(repo_id)
    
    # In a more advanced implementation, we would pass the file_tree and the `plan` 
    # to an LLM here to narrow down exactly which files are `relevant_files`.
    # For now, we simply attach the full tree to the state for the next agent.
    
    return {
        "file_tree": file_tree,
        "current_step": "repo_understanding_completed"
    }