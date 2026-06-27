import logging
from langgraph.graph import StateGraph, END
from app.models.schemas import AgentState
from app.agents import planner_node, repo_understanding_node, retrieval_node, code_generation_node, validation_node, execution_node
from app.services.ollama_client import get_ollama_llm

logger = logging.getLogger(__name__)

def route_validation(state: AgentState):
    """
    Conditional routing logic after validation.
    If valid, proceed to execution. If invalid, loop back to code generation.
    """
    validation_results = state.get("validation_results", {})
    if validation_results.get("is_valid"):
        return "execution"
    return "code_generator"

def route_execution(state: AgentState):
    """
    Conditional routing logic after execution.
    If execution passed, end the graph. If failed, loop back to code generation.
    """
    if state.get("execution_passed", False):
        return END
    
    # Check iteration count to prevent infinite loops
    iterations = state.get("iterations", 0)
    if iterations >= 3:
        logger.warning("Max iterations reached after execution failure, forcing completion.")
        return END
    
    return "code_generator"

def build_graph():
    """
    Constructs the LangGraph workflow.
    """
    logger.info("Building the AI Orchestrator LangGraph...")
    
    workflow = StateGraph(AgentState)
    
    # Initialize the LLM
    llm = get_ollama_llm()
    
    # Define Nodes
    workflow.add_node("planner", lambda state: planner_node(state, llm))
    workflow.add_node("repo_understanding", repo_understanding_node)
    workflow.add_node("retrieval", retrieval_node)
    workflow.add_node("code_generator", lambda state: code_generation_node(state, llm))
    workflow.add_node("validator", lambda state: validation_node(state, llm))
    workflow.add_node("execution", execution_node)
    
    # Define Edges
    workflow.set_entry_point("planner")
    workflow.add_edge("planner", "repo_understanding")
    workflow.add_edge("repo_understanding", "retrieval")
    workflow.add_edge("retrieval", "code_generator")
    workflow.add_edge("code_generator", "validator")
    
    # Validator → Execution (if valid) or → Code Generator (if invalid)
    workflow.add_conditional_edges(
        "validator",
        route_validation,
        {
            "execution": "execution",
            "code_generator": "code_generator"
        }
    )
    
    # Execution → END (if passed) or → Code Generator (if failed)
    workflow.add_conditional_edges(
        "execution",
        route_execution,
        {
            END: END,
            "code_generator": "code_generator"
        }
    )
    
    # Compile the graph
    app = workflow.compile()
    return app

# Singleton instance of the graph
graph_app = build_graph()