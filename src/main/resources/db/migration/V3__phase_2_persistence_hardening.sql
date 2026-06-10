CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(160) NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    response_status INT,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    UNIQUE (idempotency_key)
);

ALTER TABLE project_memberships
    ADD CONSTRAINT chk_project_memberships_role
        CHECK (role IN ('admin', 'project_lead', 'member', 'viewer'));

ALTER TABLE workflow_statuses
    ADD CONSTRAINT chk_workflow_statuses_category
        CHECK (category IN ('todo', 'in_progress', 'done'));

ALTER TABLE sprints
    ADD CONSTRAINT chk_sprints_status
        CHECK (status IN ('planned', 'active', 'completed')),
    ADD CONSTRAINT chk_sprints_date_range
        CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date);

ALTER TABLE issues
    ADD CONSTRAINT chk_issues_type
        CHECK (type IN ('epic', 'story', 'task', 'bug', 'sub_task')),
    ADD CONSTRAINT chk_issues_priority
        CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    ADD CONSTRAINT chk_issues_story_points
        CHECK (story_points IS NULL OR story_points >= 0);

ALTER TABLE custom_field_definitions
    ADD CONSTRAINT chk_custom_field_definitions_type
        CHECK (field_type IN ('text', 'number', 'dropdown', 'date'));

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_status
        CHECK (status IN ('pending', 'delivered', 'failed'));

ALTER TABLE comments
    ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(body, ''))
    ) STORED;

CREATE UNIQUE INDEX idx_sprints_one_active_per_project
    ON sprints(project_id)
    WHERE status = 'active';

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys(expires_at);
CREATE INDEX idx_comments_search ON comments USING GIN(search_vector);
CREATE INDEX idx_notifications_user_status_created ON notifications(user_id, status, created_at DESC);
CREATE INDEX idx_workflow_transitions_from ON workflow_transitions(project_id, from_status_id);
