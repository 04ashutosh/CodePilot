import logging
from fastapi import FastAPI
from app.config import settings
from app.routes import execute

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

# Mount routes
app.include_router(execute.router, prefix="/api/exec", tags=["Execution"])

@app.get("/health")
async def health_check():
    return {"service":"execution-sandbox-service","status": "up"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0",port=8084,reload=True)