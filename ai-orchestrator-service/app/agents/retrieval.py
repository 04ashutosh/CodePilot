import logging
import httpx
from typing import Dict, Any, List
from app.models.schemas import AgentState
from app.config import settings

logger = logging.getLogger(__name__)

async def semantic_search(repository_id: str, query: str, limit: int = 5) -> List[Dict[str, Any]]:
    """
    Calls the Vector Memory Service to retrieve semantically relevant code chunks.
    """
    try:
        url = f"{settings.vector_memory_url}/api/search"
        payload = {
            "repository_id": repository_id,
            "query": query,
            "limit": limit
        }
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=payload, timeout=15.0)
            response.raise_for_status()
            data = response.json()
            return data.get("results", [])
    except Exception as e:
        logger.error(f"Semantic search failed for query '{query}': {e}")
        return []

async def retrieval_node(state: AgentState) -> Dict[str, Any]:
    """
    LangGraph node for fetching relevant code snippets.
    """
    logger.info(f"--- RETRIEVAL NODE [Task: {state['task_id']}] ---")
    
    repo_id = state.get("repository_id")
    plan = state.get("plan", "")
    
    # We use the generated plan as the search query to find the most relevant context.
    # In a full production app, an LLM might extract highly specific search queries from the plan.
    search_query = plan[:500]  # Truncate to avoid massive queries
    
    retrieved_chunks = await semantic_search(repo_id, search_query)
    
    return {
        "retrieved_context": retrieved_chunks,
        "current_step": "retrieval_completed"
    }