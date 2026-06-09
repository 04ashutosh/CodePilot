from pydantic import BaseModel, Field
from typing import List, Optional

class IndexRequest(BaseModel):
    repository_id: str = Field(..., description="The UUID/ID of the repository to index")

class IndexResponse(BaseModel):
    repository_id: str
    status: str
    total_files: int
    total_chunks: int
    message: str

class SearchRequest(BaseModel):
    repository_id: str = Field(..., description="The repository collection to search in")
    query: str = Field(..., description="The natural language query or code snippet")
    limit: int = Field(default=5, ge=1, le=20, description="Max number of results to return")

class SearchResultItem(BaseModel):
    id: str
    content: str
    file_path: str
    line_start: int
    line_end: int
    language: str
    score: float

class SearchResponse(BaseModel):
    repository_id: str
    query: str
    results: List[SearchResultItem]