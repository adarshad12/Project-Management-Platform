package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issues")
public class IssueEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 32)
    private String issueKey;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 240)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private WorkflowStatusEntity status;

    @Column(nullable = false, length = 32)
    private String priority;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserEntity assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private SprintEntity sprint;

    private Integer storyPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private IssueEntity parent;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected IssueEntity() {
    }

    public IssueEntity(
            UUID id,
            ProjectEntity project,
            String issueKey,
            String type,
            String title,
            String description,
            WorkflowStatusEntity status,
            String priority,
            UserEntity assignee,
            UserEntity reporter,
            SprintEntity sprint,
            Integer storyPoints,
            IssueEntity parent,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.project = project;
        this.issueKey = issueKey;
        this.type = type;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.reporter = reporter;
        this.sprint = sprint;
        this.storyPoints = storyPoints;
        this.parent = parent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID id() {
        return id;
    }

    public String issueKey() {
        return issueKey;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String type() {
        return type;
    }

    public String priority() {
        return priority;
    }

    public WorkflowStatusEntity status() {
        return status;
    }

    public UserEntity assignee() {
        return assignee;
    }

    public UserEntity reporter() {
        return reporter;
    }

    public SprintEntity sprint() {
        return sprint;
    }

    public Integer storyPoints() {
        return storyPoints;
    }

    public IssueEntity parent() {
        return parent;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public UUID projectId() {
        return project.id();
    }

    public void updateFields(
            String title,
            String description,
            String priority,
            UserEntity assignee,
            SprintEntity sprint,
            Integer storyPoints
    ) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (priority != null) {
            this.priority = priority;
        }
        if (assignee != null) {
            this.assignee = assignee;
        }
        if (sprint != null) {
            this.sprint = sprint;
        }
        if (storyPoints != null) {
            this.storyPoints = storyPoints;
        }
        this.updatedAt = Instant.now();
    }

    public void transitionTo(WorkflowStatusEntity status, UserEntity assignee) {
        this.status = status;
        if (assignee != null) {
            this.assignee = assignee;
        }
        this.updatedAt = Instant.now();
    }

    public void moveToSprint(SprintEntity sprint) {
        this.sprint = sprint;
        this.updatedAt = Instant.now();
    }

    public void moveToBacklog() {
        this.sprint = null;
        this.updatedAt = Instant.now();
    }
}
