import logging
from langgraph.graph import StateGraph, END
from app.models.schemas import AgentState
from app.agents import planner_node, repo_understanding_node, retrieval_node, code_generation_node, validation_node
from app.services.ollama_client import get_ollama_llm

logger = logging.getLogger(__name__)

def route_validation(state: AgentState):
    """
    Conditional routing logic after validation.
    If valid, end the graph. If invalid, loop back to code generation.
    """
    validation_results = state.get("validation_results", {})
    if validation_results.get("is_valid"):
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
    
    # Define Nodes (wrap functions that need the LLM)
    workflow.add_node("planner", lambda state: planner_node(state, llm))
    workflow.add_node("repo_understanding", repo_understanding_node)
    workflow.add_node("retrieval", retrieval_node)
    workflow.add_node("code_generator", lambda state: code_generation_node(state, llm))
    workflow.add_node("validator", lambda state: validation_node(state, llm))
    
    # Define Edges (The sequence of steps)
    workflow.set_entry_point("planner")
    workflow.add_edge("planner", "repo_understanding")
    workflow.add_edge("repo_understanding", "retrieval")
    workflow.add_edge("retrieval", "code_generator")
    workflow.add_edge("code_generator", "validator")
    
    # Conditional Edge for the Validation loop
    workflow.add_conditional_edges(
        "validator",
        route_validation,
        {
            END: END,
            "code_generator": "code_generator" # Loop back on failure
        }
    )
    
    # Compile the graph
    app = workflow.compile()
    return app

# Singleton instance of the graph
graph_app = build_graph()