import logging
from typing import Dict, Any
from langchain_core.messages import SystemMessage, HumanMessage
from app.models.schemas import AgentState

logger = logging.getLogger(__name__)

VALIDATION_PROMPT = """You are an expert code reviewer.
Review the proposed code changes. Does it fulfill the task requirements? Does it contain syntax errors or bad practices?
If the code is correct and fully fulfills the task, respond with exactly: "VALID".
If there are errors or missing parts, explain the errors clearly so the code generator can fix them.
"""

def validation_node(state: AgentState, llm: Any) -> Dict[str, Any]:
    """
    LangGraph node for validating the generated code.
    """
    logger.info(f"--- VALIDATION NODE [Task: {state['task_id']}] ---")
    
    task_description = state.get("description", "")
    generated_code = state.get("generated_code", "")
    iterations = state.get("iterations", 0) + 1
    
    # We enforce a maximum of 3 refinement loops to prevent infinite loops.
    if iterations >= 3:
        logger.warning(f"Max iterations reached for task {state['task_id']}")
        return {
            "validation_results": {"is_valid": True, "feedback": "Max iterations reached, forcing completion."},
            "iterations": iterations,
            "current_step": "validation_completed"
        }
        
    user_message = (
        f"Original Task:\n{task_description}\n\n"
        f"Generated Code:\n{generated_code}\n\n"
        f"Please review the code."
    )
    
    messages = [
        SystemMessage(content=VALIDATION_PROMPT),
        HumanMessage(content=user_message)
    ]
    
    response = llm.invoke(messages)
    feedback = response.content.strip()
    
    # If the LLM says "VALID", the code is approved.
    is_valid = "VALID" in feedback.upper()
    
    # Track errors if it failed validation
    errors = state.get("errors", [])
    if not is_valid:
        errors.append(feedback)
        
    return {
        "validation_results": {"is_valid": is_valid, "feedback": feedback},
        "errors": errors,
        "iterations": iterations,
        "current_step": "validation_completed"
    }