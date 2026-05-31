-- This runs on first PostgreSQL startup.
-- The auth database is auto-created by POSTGRES_DB env var.

CREATE DATABASE codepilot_project;

GRANT ALL PRIVILEGES ON DATABASE codepilot_project TO codepilot;