import logging
from fastapi import APIRouter, HTTPException, status
from app.models.schemas import SearchRequest, SearchResponse, SearchResultItem
from app.services.embedder import generate_embedding
from app.services.chroma_client import get_or_create_collection

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("", response_model=SearchResponse)
async def search_repository(request: SearchRequest):
    """
    POST /api/search
    Performs semantic search over a repository's code chunks in ChromaDB.
    """
    repo_id = request.repository_id
    query = request.query
    limit = request.limit
    
    logger.info("Semantic search in repo %s (query: '%s', limit: %d)", repo_id, query, limit)
    
    try:
        collection = get_or_create_collection(repo_id)
        
        # Return early if there's no data indexed
        if collection.count() == 0:
            logger.warning("Search requested on empty or unindexed collection for repo %s", repo_id)
            return SearchResponse(
                repository_id=repo_id,
                query=query,
                results=[]
            )
            
        # 1. Generate query embedding
        query_embedding = generate_embedding(query)
        
        # 2. Query ChromaDB
        results = collection.query(
            query_embeddings=[query_embedding],
            n_results=limit,
            include=["documents", "metadatas", "distances"]
        )
        
        # 3. Format results
        search_results = []
        if results and "ids" in results and results["ids"] and results["ids"][0]:
            ids = results["ids"][0]
            documents = results["documents"][0]
            metadatas = results["metadatas"][0]
            distances = results["distances"][0] if "distances" in results else [0.0] * len(ids)
            
            for idx in range(len(ids)):
                distance = distances[idx]
                # Convert distance (L2 metric) to a similarity score range [0, 1]
                score = 1.0 / (1.0 + distance)
                
                meta = metadatas[idx]
                search_results.append(
                    SearchResultItem(
                        id=ids[idx],
                        content=documents[idx],
                        file_path=meta.get("file_path", "unknown"),
                        line_start=meta.get("line_start", 1),
                        line_end=meta.get("line_end", 1),
                        language=meta.get("language", "unknown"),
                        score=round(score, 4)
                    )
                )
                
        return SearchResponse(
            repository_id=repo_id,
            query=query,
            results=search_results
        )
        
    except Exception as e:
        logger.error("Error during semantic search in repo %s: %s", repo_id, str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to perform semantic search: {str(e)}"
        )