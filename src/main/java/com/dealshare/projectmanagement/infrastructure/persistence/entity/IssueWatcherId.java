package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class IssueWatcherId implements Serializable {

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "user_id")
    private UUID userId;

    protected IssueWatcherId() {
    }

    public IssueWatcherId(UUID issueId, UUID userId) {
        this.issueId = issueId;
        this.userId = userId;
    }

    public UUID issueId() {
        return issueId;
    }

    public UUID userId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IssueWatcherId that)) {
            return false;
        }
        return Objects.equals(issueId, that.issueId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, userId);
    }
}
