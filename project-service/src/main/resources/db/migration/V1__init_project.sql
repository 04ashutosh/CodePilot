CREATE TYPE task_status AS ENUM (
    'PENDING', 'PLANNING', 'ANALYZING', 'RETRIEVING',
    'GENERATING', 'VALIDATING', 'COMPLETED', 'FAILED', 'CANCELLED'
);

CREATE TYPE task_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

CREATE TABLE projects (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          name            VARCHAR(200) NOT NULL,
                          description     TEXT,
                          workspace_id    UUID NOT NULL,
                          owner_id        UUID NOT NULL,
                          is_active       BOOLEAN DEFAULT true,
                          created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE repositories (
                              id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                              name            VARCHAR(200) NOT NULL,
                              url             VARCHAR(500),
                              local_path      VARCHAR(500),
                              branch          VARCHAR(100) DEFAULT 'main',
                              last_synced_at  TIMESTAMP WITH TIME ZONE,
                              framework       VARCHAR(100),
                              language        VARCHAR(100),
                              metadata        JSONB DEFAULT '{}',
                              created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                              updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE tasks (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                       repository_id   UUID REFERENCES repositories(id),
                       title           VARCHAR(500) NOT NULL,
                       description     TEXT NOT NULL,
                       status          task_status DEFAULT 'PENDING',
                       priority        task_priority DEFAULT 'MEDIUM',
                       created_by      UUID NOT NULL,
                       assigned_to     UUID,
                       result          JSONB DEFAULT '{}',
                       diff_content    TEXT,
                       explanation     TEXT,
                       started_at      TIMESTAMP WITH TIME ZONE,
                       completed_at    TIMESTAMP WITH TIME ZONE,
                       created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE execution_history (
                                   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   task_id         UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                                   step_name       VARCHAR(100) NOT NULL,
                                   step_order      INTEGER NOT NULL,
                                   status          VARCHAR(50) NOT NULL,
                                   input_data      JSONB DEFAULT '{}',
                                   output_data     JSONB DEFAULT '{}',
                                   error_message   TEXT,
                                   duration_ms     BIGINT,
                                   started_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                   completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_projects_workspace ON projects(workspace_id);
CREATE INDEX idx_projects_owner ON projects(owner_id);
CREATE INDEX idx_repositories_project ON repositories(project_id);
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_created_by ON tasks(created_by);
CREATE INDEX idx_execution_history_task ON execution_history(task_id);