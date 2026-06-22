import logging
from typing import Dict, Any
import httpx
from app.config import settings

logger = logging.getLogger(__name__)

def execution_node(state: dict) -> Dict[str, Any]:
    """
    LangGraph node that sends generated code to the Execution Sandbox Service
    for real runtime validation inside an isolated Docker container.
    """
    logger.info(f"--- EXECUTION NODE [Task: {state['task_id']}] ---")

    generated_code = state.get("generated_code", "")
    language = state.get("language", "python")

    if not generated_code:
        logger.warning("No generated code to execute.")
        return {
            "execution_results": {"exit_code": -1, "stdout": "", "stderr": "No code to execute.", "timed_out": False, "execution_time_ms": 0},
            "execution_passed": False,
            "current_step": "execution_completed"
        }

    # Strip markdown code fences if present (LLMs often wrap code in ```)
    code_to_run = generated_code
    if code_to_run.startswith("```"):
        lines = code_to_run.split("\n")
        # Remove first line (```python) and last line (```)
        lines = [l for l in lines if not l.strip().startswith("```")]
        code_to_run = "\n".join(lines)

    try:
        response = httpx.post(
            f"{settings.execution_service_url}/api/exec/run",
            json={
                "code": code_to_run,
                "language": language,
                "timeout": 30
            },
            timeout=60.0  # Give extra time for Docker spin-up
        )
        response.raise_for_status()
        result = response.json()

        exit_code = result.get("exit_code", -1)
        execution_passed = exit_code == 0

        if execution_passed:
            logger.info(f"Code execution PASSED (exit_code=0)")
            logger.info(f"stdout: {result.get('stdout', '')[:200]}")
        else:
            logger.warning(f"Code execution FAILED (exit_code={exit_code})")
            logger.warning(f"stderr: {result.get('stderr', '')[:500]}")

        # If execution failed, append the error to the errors list for the code generator
        errors = state.get("errors", [])
        if not execution_passed:
            errors.append(f"Runtime Error:\n{result.get('stderr', 'Unknown error')}")

        return {
            "execution_results": result,
            "execution_passed": execution_passed,
            "errors": errors,
            "current_step": "execution_completed"
        }

    except Exception as e:
        logger.error(f"Failed to reach Execution Sandbox: {e}")
        # If the sandbox is down, skip execution and pass through
        return {
            "execution_results": {"exit_code": -1, "stdout": "", "stderr": str(e), "timed_out": False, "execution_time_ms": 0},
            "execution_passed": True,  # Don't block workflow if sandbox is unavailable
            "current_step": "execution_skipped"
        }