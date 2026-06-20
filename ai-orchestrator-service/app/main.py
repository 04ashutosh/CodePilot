import logging
from fastapi import FastAPI, BackgroundTasks
from app.config import settings
from app.models.schemas import TaskRequest, TaskResponse
from app.graph.workflow import graph_app

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

@app.get("/health")
async def health_check():
    return {"service": "ai-orchestrator-service", "status": "up"}

async def run_ai_workflow(task_data: dict):
    """
    Background task to execute the LangGraph workflow.
    """
    task_id = task_data.get("task_id")
    logger.info(f"Starting AI workflow for task: {task_id}")
    
    # Initialize state
    initial_state = {
        "task_id": task_id,
        "repository_id": task_data.get("repository_id"),
        "title": task_data.get("title"),
        "description": task_data.get("description"),
        "file_tree": None,
        "relevant_files": [],
        "retrieved_context": [],
        "plan": None,
        "generated_code": None,
        "validation_results": None,
        "current_step": "started",
        "errors": [],
        "iterations": 0
    }
    
    try:
        # Execute the graph asynchronously using .ainvoke
        final_state = await graph_app.ainvoke(initial_state)
        
        logger.info(f"Workflow completed successfully for task: {task_id}")
        logger.info(f"==== FINAL GENERATED CODE ====\n{final_state.get('generated_code')}")
        
        # Note: In Phase 4/5, we will publish this result back to Kafka/Project Service.
        
    except Exception as e:
        logger.error(f"Workflow failed for task {task_id}: {e}")

@app.post("/api/ai/tasks/submit", response_model=TaskResponse)
async def submit_task(request: TaskRequest, background_tasks: BackgroundTasks):
    """
    Endpoint to receive tasks from the API Gateway / Project Service.
    """
    logger.info(f"Received task submission: {request.task_id}")
    
    # Prepare initial state data
    task_data = {
        "task_id": request.task_id,
        "repository_id": request.repository_id,
        "title": request.title,
        "description": request.description
    }
    
    # Trigger background workflow execution
    background_tasks.add_task(run_ai_workflow, task_data)
    
    return TaskResponse(
        task_id=request.task_id,
        status="ACCEPTED",
        message="Task submitted to AI orchestrator successfully."
    )

if __name__ == "__main__":
    import uvicorn
    # Run the application locally
    uvicorn.run("app.main:app", host="0.0.0.0", port=8090, reload=True)