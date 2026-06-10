CREATE TABLE board_issue_read_model (
    issue_id UUID PRIMARY KEY REFERENCES issues(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    issue_key VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(240) NOT NULL,
    description TEXT,
    status_id UUID NOT NULL REFERENCES workflow_statuses(id),
    status_name VARCHAR(80) NOT NULL,
    status_position INT NOT NULL,
    priority VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL,
    assignee_id UUID REFERENCES users(id),
    assignee_name VARCHAR(160),
    reporter_id UUID NOT NULL REFERENCES users(id),
    reporter_name VARCHAR(160) NOT NULL,
    sprint_id UUID REFERENCES sprints(id),
    sprint_name VARCHAR(160),
    sprint_status VARCHAR(32),
    story_points INT,
    parent_issue_key VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    read_model_updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO board_issue_read_model (
    issue_id,
    project_id,
    issue_key,
    type,
    title,
    description,
    status_id,
    status_name,
    status_position,
    priority,
    version,
    assignee_id,
    assignee_name,
    reporter_id,
    reporter_name,
    sprint_id,
    sprint_name,
    sprint_status,
    story_points,
    parent_issue_key,
    created_at,
    updated_at
)
SELECT
    i.id,
    i.project_id,
    i.issue_key,
    i.type,
    i.title,
    i.description,
    ws.id,
    ws.name,
    ws.position,
    i.priority,
    i.version,
    assignee.id,
    assignee.display_name,
    reporter.id,
    reporter.display_name,
    s.id,
    s.name,
    s.status,
    i.story_points,
    parent.issue_key,
    i.created_at,
    i.updated_at
FROM issues i
JOIN workflow_statuses ws ON ws.id = i.status_id
JOIN users reporter ON reporter.id = i.reporter_id
LEFT JOIN users assignee ON assignee.id = i.assignee_id
LEFT JOIN sprints s ON s.id = i.sprint_id
LEFT JOIN issues parent ON parent.id = i.parent_id;

CREATE INDEX idx_board_issue_read_model_project_status
    ON board_issue_read_model(project_id, status_position, created_at DESC);

CREATE INDEX idx_board_issue_read_model_project_updated
    ON board_issue_read_model(project_id, updated_at DESC);
