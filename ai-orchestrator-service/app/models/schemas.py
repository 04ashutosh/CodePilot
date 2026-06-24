from typing import TypedDict, List, Dict, Any, Optional
from pydantic import BaseModel

# ---------------------------------------------------------
# API Request/Response Schemas
# ---------------------------------------------------------

class TaskRequest(BaseModel):
    task_id: str
    project_id: str
    repository_id: str
    title: str
    description: str

class TaskResponse(BaseModel):
    task_id: str
    status: str
    message: str

# ---------------------------------------------------------
# LangGraph State Schema
# ---------------------------------------------------------

class AgentState(TypedDict):
    """
    The state dictionary that gets passed between nodes in the LangGraph workflow.
    Each agent reads from and writes to this state.
    """
    # Input parameters
    task_id: str
    repository_id: str
    title: str
    description: str
    
    # Accumulated context
    file_tree: Optional[Dict[str, Any]]
    relevant_files: List[str]
    retrieved_context: List[Dict[str, Any]]
    
    # Agent Outputs
    plan: Optional[str]
    generated_code: Optional[str]
    validation_results: Optional[Dict[str, Any]]
    
    # Execution Sandbox (Phase 4.2)
    execution_results: Optional[Dict[str, Any]]
    execution_passed: bool
    language: str
    
    # Workflow status tracking
    current_step: str
    errors: List[str]
    iterations: int