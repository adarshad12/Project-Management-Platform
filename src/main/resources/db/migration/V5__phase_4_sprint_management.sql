ALTER TABLE sprints
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN completed_story_points INT NOT NULL DEFAULT 0,
    ADD COLUMN carried_over_story_points INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_sprints_completed_story_points
        CHECK (completed_story_points >= 0),
    ADD CONSTRAINT chk_sprints_carried_over_story_points
        CHECK (carried_over_story_points >= 0);

CREATE TABLE sprint_completion_issues (
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    outcome VARCHAR(32) NOT NULL,
    target_sprint_id UUID REFERENCES sprints(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (sprint_id, issue_id),
    CONSTRAINT chk_sprint_completion_issues_outcome
        CHECK (outcome IN ('completed', 'carried_over', 'moved_to_backlog'))
);

CREATE INDEX idx_sprint_completion_issues_target ON sprint_completion_issues(target_sprint_id);
