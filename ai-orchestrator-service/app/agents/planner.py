import logging
from typing import Dict, Any
from langchain_core.messages import SystemMessage, HumanMessage
from app.models.schemas import AgentState

logger = logging.getLogger(__name__)

# System prompt for the Planner Agent
PLANNER_PROMPT = """You are an expert software engineering planner.
Your job is to take a task description and break it down into a clear, step-by-step implementation plan.
Output ONLY the plan. Be concise, focus on technical implementation details, and list the files that will likely need to be modified.
"""

def planner_node(state: AgentState, llm: Any) -> Dict[str, Any]:
    """
    LangGraph node for generating an implementation plan.
    """
    logger.info(f"--- PLANNER NODE [Task: {state['task_id']}] ---")
    
    title = state.get("title", "")
    description = state.get("description", "")
    
    messages = [
        SystemMessage(content=PLANNER_PROMPT),
        HumanMessage(content=f"Task Title: {title}\nTask Description: {description}")
    ]
    
    # We will inject the Ollama LLM instance into the graph during Phase 3.5.
    response = llm.invoke(messages)
    
    # Return ONLY the keys of the AgentState we want to update.
    return {
        "plan": response.content,
        "current_step": "planning_completed"
    }