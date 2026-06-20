from fastapi import FastAPI, Query
import uvicorn

app = FastAPI(title="Mock Repository Service")

@app.get("/api/repos/{repo_id}/tree")
async def get_tree(repo_id: str):
    # Returns a mock file tree containing two files
    return {
        "repositoryId": repo_id,
        "root": {
            "name": "root",
            "path": "",
            "type": "DIRECTORY",
            "children": [
                {
                    "name": "auth_service.py",
                    "path": "auth_service.py",
                    "type": "FILE"
                },
                {
                    "name": "utils.py",
                    "path": "utils.py",
                    "type": "FILE"
                }
            ]
        }
    }

@app.get("/api/repos/{repo_id}/files")
async def get_file_content(repo_id: str, path: str = Query(...)):
    # Returns mock source code contents depending on the file path requested
    if path == "auth_service.py":
        content = """import jwt
import datetime

def generate_token(user_id: str) -> str:
    \"\"\"Generates a JWT token for the given user_id expiring in 1 hour.\"\"\"
    payload = {
        'sub': user_id,
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=1)
    }
    return jwt.encode(payload, 'secret', algorithm='HS256')

def verify_token(token: str) -> dict:
    \"\"\"Verifies the JWT token and returns the payload.\"\"\"
    try:
        return jwt.decode(token, 'secret', algorithms=['HS256'])
    except jwt.ExpiredSignatureError:
        return {'error': 'token expired'}
    except jwt.InvalidTokenError:
        return {'error': 'invalid token'}
"""
        language = "python"
    elif path == "utils.py":
        content = """def calculate_addition(a: int, b: int) -> int:
    \"\"\"Returns the sum of two integers.\"\"\"
    return a + b

def calculate_subtraction(a: int, b: int) -> int:
    \"\"\"Returns the difference between two integers.\"\"\"
    return a - b
"""
        language = "python"
    else:
        content = ""
        language = "unknown"

    return {
        "repositoryId": repo_id,
        "filePath": path,
        "content": content,
        "lineCount": len(content.splitlines()),
        "size": len(content),
        "language": language
    }

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8083)