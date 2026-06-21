from typing import Optional
from pydantic import BaseModel, Field

class ExecutionRequest(BaseModel):
    code: str = Field(..., description="The source code to execute.")
    language: str = Field(...,description="Programming language: 'python' or 'javascript' or 'java'.")
    timeout: int = Field(default=30,description="Maximum execution time in seconds.")

class ExecutionResponse(BaseModel):
    exit_code: int
    stdout: str
    stderr: str
    timed_out: bool
    execution_time_ms: int