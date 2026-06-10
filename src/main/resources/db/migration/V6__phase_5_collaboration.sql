CREATE INDEX idx_issue_watchers_user ON issue_watchers(user_id);
CREATE INDEX idx_comments_parent_created ON comments(parent_comment_id, created_at ASC);
CREATE INDEX idx_activity_actor_created ON activity_log(actor_id, created_at DESC);
CREATE INDEX idx_activity_issue_created ON activity_log(issue_id, created_at DESC);
CREATE INDEX idx_activity_event_created ON activity_log(project_id, event_type, created_at DESC);

ALTER TABLE notifications
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_error TEXT,
    ADD COLUMN next_attempt_at TIMESTAMPTZ;

CREATE INDEX idx_notifications_retry
    ON notifications(status, next_attempt_at, created_at)
    WHERE status IN ('pending', 'failed');
