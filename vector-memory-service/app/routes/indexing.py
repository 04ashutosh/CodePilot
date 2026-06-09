import logging
import httpx
from fastapi import APIRouter, Header, HTTPException, status
from app.config import get_settings
from app.models.schemas import IndexRequest, IndexResponse
from app.services.chunker import chunk_code
from app.services.embedder import generate_embeddings
from app.services.chroma_client import get_or_create_collection, delete_collection

logger = logging.getLogger(__name__)
router = APIRouter()

def extract_files(node: dict) -> list[str]:
    """Recursively extracts all file paths from the repository file tree node."""
    files = []
    if not node:
        return files
        
    node_type = node.get("type")
    if isinstance(node_type, str):
        node_type = node_type.upper()
        
    if node_type == "FILE":
        if node.get("path"):
            files.append(node["path"])
    elif node_type in ["DIRECTORY", "DIR"] or node.get("children"):
        for child in node.get("children", []):
            files.extend(extract_files(child))
            
    return files

@router.post("/index", response_model=IndexResponse)
async def index_repository(
    request: IndexRequest,
    authorization: str = Header(None)
):
    """
    POST /api/search/index
    Fetches the file tree and contents from repository-service, chunks them,
    generates vector embeddings, and stores them in ChromaDB.
    """
    settings = get_settings()
    repo_id = request.repository_id
    
    # 1. Prepare Authorization / Gateway headers
    headers = {}
    if authorization:
        headers["Authorization"] = authorization
    else:
        # Fallback to internal API key if no token is passed
        headers["X-Internal-Key"] = settings.internal_api_key
        
    tree_url = f"{settings.repository_service_url}/api/repos/{repo_id}/tree"
    logger.info("Fetching file tree for repo %s from %s", repo_id, tree_url)
    
    # 2. Fetch the File Tree
    async with httpx.AsyncClient(timeout=30.0) as http_client:
        try:
            tree_response = await http_client.get(tree_url, headers=headers)
            if tree_response.status_code != 200:
                logger.error("Failed to fetch file tree: %d - %s", tree_response.status_code, tree_response.text)
                raise HTTPException(
                    status_code=tree_response.status_code,
                    detail=f"Failed to fetch repository tree: {tree_response.text}"
                )
            tree_data = tree_response.json()
        except httpx.RequestError as e:
            logger.error("HTTP error fetching file tree: %s", str(e))
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=f"Repository Service unreachable: {str(e)}"
            )

    root_node = tree_data.get("root")
    if not root_node:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Repository tree has no root node"
        )
        
    file_paths = extract_files(root_node)
    logger.info("Found %d files to index for repo %s", len(file_paths), repo_id)
    
    if not file_paths:
        return IndexResponse(
            repository_id=repo_id,
            status="completed",
            total_files=0,
            total_chunks=0,
            message="No files found in repository to index."
        )

    # 3. Fetch each file's content and chunk it
    all_chunks = []
    files_indexed = 0
    
    async with httpx.AsyncClient(timeout=15.0) as http_client:
        for file_path in file_paths:
            file_url = f"{settings.repository_service_url}/api/repos/{repo_id}/files"
            try:
                file_response = await http_client.get(
                    file_url, 
                    params={"path": file_path}, 
                    headers=headers
                )
                if file_response.status_code != 200:
                    logger.warning("Skipping file %s: HTTP %d", file_path, file_response.status_code)
                    continue
                    
                file_data = file_response.json()
                content = file_data.get("content")
                language = file_data.get("language") or "unknown"
                
                if not content or not content.strip():
                    continue
                    
                file_chunks = chunk_code(
                    content,
                    max_lines=settings.chunk_max_lines,
                    overlap_lines=settings.chunk_overlap_lines
                )
                
                for idx, chunk in enumerate(file_chunks):
                    all_chunks.append({
                        "id": f"{repo_id}_{file_path}_{idx}",
                        "content": chunk["content"],
                        "file_path": file_path,
                        "language": language,
                        "line_start": chunk["line_start"],
                        "line_end": chunk["line_end"]
                    })
                files_indexed += 1
                
            except Exception as e:
                logger.error("Error processing file %s: %s", file_path, str(e))
                continue

    logger.info("Generated %d chunks from %d files", len(all_chunks), files_indexed)
    
    if not all_chunks:
        return IndexResponse(
            repository_id=repo_id,
            status="completed",
            total_files=files_indexed,
            total_chunks=0,
            message="No text chunks generated from files."
        )

    # 4. Generate Embeddings for all chunks
    logger.info("Generating embeddings for %d chunks...", len(all_chunks))
    chunk_texts = [c["content"] for c in all_chunks]
    embeddings = generate_embeddings(chunk_texts)
    
    # 5. Clear old and insert into ChromaDB
    delete_collection(repo_id)
    collection = get_or_create_collection(repo_id)
    
    ids = [c["id"] for c in all_chunks]
    documents = chunk_texts
    metadatas = [
        {
            "repository_id": repo_id,
            "file_path": c["file_path"],
            "language": c["language"],
            "line_start": c["line_start"],
            "line_end": c["line_end"]
        }
        for c in all_chunks
    ]
    
    # Write to ChromaDB in batches of 500 to keep requests clean
    batch_size = 500
    for i in range(0, len(ids), batch_size):
        end_pos = min(i + batch_size, len(ids))
        collection.add(
            ids=ids[i:end_pos],
            embeddings=embeddings[i:end_pos],
            documents=documents[i:end_pos],
            metadatas=metadatas[i:end_pos]
        )
        
    logger.info("Successfully indexed %d chunks in ChromaDB for repo %s", len(ids), repo_id)
    
    return IndexResponse(
        repository_id=repo_id,
        status="completed",
        total_files=files_indexed,
        total_chunks=len(ids),
        message=f"Successfully indexed {files_indexed} files into {len(ids)} chunks."
    )