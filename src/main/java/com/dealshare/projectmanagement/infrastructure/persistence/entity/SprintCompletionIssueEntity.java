package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sprint_completion_issues")
public class SprintCompletionIssueEntity {

    @EmbeddedId
    private SprintCompletionIssueId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("sprintId")
    @JoinColumn(name = "sprint_id", nullable = false)
    private SprintEntity sprint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("issueId")
    @JoinColumn(name = "issue_id", nullable = false)
    private IssueEntity issue;

    @Column(nullable = false, length = 32)
    private String outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_sprint_id")
    private SprintEntity targetSprint;

    @Column(nullable = false)
    private Instant createdAt;

    protected SprintCompletionIssueEntity() {
    }

    public SprintCompletionIssueEntity(
            SprintCompletionIssueId id,
            SprintEntity sprint,
            IssueEntity issue,
            String outcome,
            SprintEntity targetSprint,
            Instant createdAt
    ) {
        this.id = id;
        this.sprint = sprint;
        this.issue = issue;
        this.outcome = outcome;
        this.targetSprint = targetSprint;
        this.createdAt = createdAt;
    }
}
