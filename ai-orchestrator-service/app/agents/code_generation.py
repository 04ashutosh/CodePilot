import logging
from typing import Dict, Any
from langchain_core.messages import SystemMessage, HumanMessage
from app.models.schemas import AgentState

logger = logging.getLogger(__name__)

CODE_GEN_PROMPT = """You are an expert AI software engineer.
You are given a task, an implementation plan, and relevant code context from the repository.
Write the exact code modifications required to complete the task. 
If modifying existing files, output the file name and the complete modified code block.
If creating new files, output the file name and the code.
Output ONLY code and standard markdown formatting. No conversational filler.
"""

def code_generation_node(state: AgentState, llm: Any) -> Dict[str, Any]:
    """
    LangGraph node for generating the actual code modifications.
    """
    logger.info(f"--- CODE GENERATION NODE [Task: {state['task_id']}] ---")
    
    plan = state.get("plan", "")
    context_chunks = state.get("retrieved_context", [])
    
    # Format the retrieved context into a readable string for the LLM
    context_str = "\n".join([
        f"File Chunk: {chunk.get('id', 'Unknown')}\nCode:\n{chunk.get('content', '')}\n---" 
        for chunk in context_chunks
    ])
    
    user_message = (
        f"Implementation Plan:\n{plan}\n\n"
        f"Relevant Existing Code:\n{context_str}\n\n"
        f"Please write the required code modifications."
    )
    
    messages = [
        SystemMessage(content=CODE_GEN_PROMPT),
        HumanMessage(content=user_message)
    ]
    
    # Generate the code using Ollama
    response = llm.invoke(messages)
    
    return {
        "generated_code": response.content,
        "current_step": "code_generation_completed"
    }