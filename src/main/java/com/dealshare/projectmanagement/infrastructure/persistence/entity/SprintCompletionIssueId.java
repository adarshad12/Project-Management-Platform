package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SprintCompletionIssueId implements Serializable {

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "issue_id")
    private UUID issueId;

    protected SprintCompletionIssueId() {
    }

    public SprintCompletionIssueId(UUID sprintId, UUID issueId) {
        this.sprintId = sprintId;
        this.issueId = issueId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SprintCompletionIssueId that)) {
            return false;
        }
        return Objects.equals(sprintId, that.sprintId) && Objects.equals(issueId, that.issueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sprintId, issueId);
    }
}
