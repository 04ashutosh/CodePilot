import logging
import tempfile
import time
import os
import docker
from docker.errors import ContainerError, ImageNotFound, APIError
from app.config import settings

logger = logging.getLogger(__name__)

# Map language to Docker image and run command
LANGUAGE_CONFIG = {
    "python":{
        "image": "python:3.12-slim",
        "file_ext": ".py",
        "command": lambda filename: ["python",filename]
    },
    "javascript":{
        "image": "node:20-slim",
        "file_ext": ".js",
        "command": lambda filename: ["node",filename]
    },
    "java": {
        "image": "openjdk:21-slim",
        "file_ext": ".java",
        "command": lambda filename: [
            "sh","-c",
            f"javac {filename} && java {filename.replace('.java','')}"
        ]
    }
}

def execute_code(code: str, language: str, timeout: int = 30) -> dict:
    """
    Executes the given code inside an isolated Docker container.
    Returns exit_code, stdout,stderr,timeed_out, and execution_time_ms.
    """

    language = language.lower()

    if language not in LANGUAGE_CONFIG:
        return {
            "exit_code": -1,
            "stdout": "",
            "stderr": f"Unsupported language: '{language}'. Supported: {list(LANGUAGE_CONFIG.keys())}",
            "timed_out": False,
            "execution_time_ms": 0
        }
    
    config = LANGUAGE_CONFIG[language]
    client = docker.from_env()

    # Create a temporary file with the code
    tmp_dir = tempfile.mkdtemp()
    filename = f"code{config['file_ext']}"
    filepath = os.path.join(tmp_dir,filename)

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(code)

    logger.info(f"Executing {language} code in sandbox (timeout={timeout}s)")

    container = None
    timed_out = False
    start_time = time.time()

    try:
        # Pull the image if it doesn't exist locally
        try:
            client.images.get(config["image"])
        except ImageNotFound:
            logger.info(f"Pulling Docker image: {config['image']}")
            client.images.pull(config["image"])

        # Run the container with strict resource limits
        container = client.containers.run(
            image=config["image"],
            command=config["command"](f"/sandbox/{filename}"),
            volumes={tmp_dir: {"bind": "/sandbox","mode":"ro"}},
            mem_limit=settings.max_memory,
            nano_cpus=int(settings.max_cpus*1e9),
            network_mode="none", # No network access
            read_only=True,      # Read-only filesystem
            tmpfs={"/tmp":"size=32m"}, # Small writable /tmp
            detach=True,
            stdout=True,
            stderr=True
        )

        # Wait for the container to finish (with timeout)
        result = container.wait(timeout=timeout)
        exit_code = result.get("StatusCode",-1)

        stdout = container.logs(stdout=True,stderr=False).decode("utf-8",errors="replace")
        stderr = container.logs(stdout=False,stderr=True).decode("utf-8",errors="replace")

    except Exception as e:
        error_msg = str(e)
        # Check if it was a timeout
        if "timed out" in error_msg.lower() or "read timeout" in error_msg.lower():
            timed_out = True
            exit_code = -1
            stdout = ""
            stderr = f"Execution timed out after {timeout} seconds."
            logger.warning(f"Code execution timed out after {timeout}s")
        else:
            exit_code = -1
            stdout = ""
            stderr = f"Docker execution error: {error_msg}"
            logger.error(f"Docker execution failed: {error_msg}")
    
    finally:
        elapsed_ms = int((time.time()-start_time)*1000)

        # Always clean up the container
        if container:
            try:
                container.remove(force=True)
            except Exception:
                pass
        
        # Clean up temp file
        try:
            os.remove(filepath)
            os.rmdir(tmp_dir)
        except Exception:
            pass

    return {
        "exit_code": exit_code,
        "stdout": stdout.strip(),
        "stderr": stderr.strip(),
        "timed_out": timed_out,
        "execution_time_ms": elapsed_ms
    }