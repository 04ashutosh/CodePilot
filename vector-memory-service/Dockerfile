# ============================================
# Vector Memory Service — Production Dockerfile
# ============================================
# Multi-stage build:
# 1. Install Python dependencies (cached unless requirements.txt changes).
# 2. Copy application code.
# 3. Run with uvicorn.

FROM python:3.12-slim AS base

# Prevent Python from writing .pyc files and enable unbuffered output.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# ===== Stage 1: Install dependencies =====
# This layer is cached unless requirements.txt changes.
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# ===== Stage 2: Copy application code =====
COPY app/ ./app/

# ===== Health check =====
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8091/health')" || exit 1

# ===== Expose port =====
EXPOSE 8091

# ===== Run the application =====
# --workers 2: Two worker processes for handling concurrent requests.
# --host 0.0.0.0: Bind to all interfaces (required inside Docker).
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8091", "--workers", "2"]