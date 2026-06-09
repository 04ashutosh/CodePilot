import logging
from typing import List, Dict, Any

logger = logging.getLogger(__name__)

def chunk_code(
    content: str, 
    max_lines: int = 50, 
    overlap_lines: int = 10
) -> List[Dict[str, Any]]:
    """
    Splits source code content into overlapping chunks of lines.
    
    Args:
        content: The raw file content string.
        max_lines: Maximum number of lines in a single chunk.
        overlap_lines: Number of lines to overlap between consecutive chunks.
        
    Returns:
        A list of dicts, each representing a chunk:
        {
            "content": str,
            "line_start": int, (1-indexed)
            "line_end": int (1-indexed)
        }
    """
    if not content or not content.strip():
        return []
        
    lines = content.splitlines()
    total_lines = len(lines)
    chunks = []
    
    # If the file is smaller than our max chunk size, return it as a single chunk
    if total_lines <= max_lines:
        return [
            {
                "content": content,
                "line_start": 1,
                "line_end": total_lines
            }
        ]
        
    step = max_lines - overlap_lines
    if step <= 0:
        step = max_lines // 2 or 1
        
    i = 0
    while i < total_lines:
        start_idx = i
        end_idx = min(i + max_lines, total_lines)
        
        chunk_lines = lines[start_idx:end_idx]
        chunk_content = "\n".join(chunk_lines)
        
        chunks.append({
            "content": chunk_content,
            "line_start": start_idx + 1,
            "line_end": end_idx
        })
        
        if end_idx == total_lines:
            break
            
        i += step
        
    logger.debug("Split code (%d lines) into %d chunks", total_lines, len(chunks))
    return chunks