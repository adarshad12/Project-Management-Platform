package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "board_issue_read_model")
public class BoardIssueReadModelEntity {

    @Id
    private UUID issueId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 32)
    private String issueKey;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 240)
    private String title;

    private String description;

    @Column(nullable = false)
    private UUID statusId;

    @Column(nullable = false, length = 80)
    private String statusName;

    @Column(nullable = false)
    private int statusPosition;

    @Column(nullable = false, length = 32)
    private String priority;

    @Column(nullable = false)
    private long version;

    private UUID assigneeId;

    private String assigneeName;

    @Column(nullable = false)
    private UUID reporterId;

    @Column(nullable = false)
    private String reporterName;

    private UUID sprintId;

    private String sprintName;

    private String sprintStatus;

    private Integer storyPoints;

    private String parentIssueKey;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant readModelUpdatedAt;

    protected BoardIssueReadModelEntity() {
    }

    public BoardIssueReadModelEntity(
            UUID issueId,
            UUID projectId,
            String issueKey,
            String type,
            String title,
            String description,
            UUID statusId,
            String statusName,
            int statusPosition,
            String priority,
            long version,
            UUID assigneeId,
            String assigneeName,
            UUID reporterId,
            String reporterName,
            UUID sprintId,
            String sprintName,
            String sprintStatus,
            Integer storyPoints,
            String parentIssueKey,
            Instant createdAt,
            Instant updatedAt,
            Instant readModelUpdatedAt
    ) {
        this.issueId = issueId;
        this.projectId = projectId;
        this.issueKey = issueKey;
        this.type = type;
        this.title = title;
        this.description = description;
        this.statusId = statusId;
        this.statusName = statusName;
        this.statusPosition = statusPosition;
        this.priority = priority;
        this.version = version;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.sprintId = sprintId;
        this.sprintName = sprintName;
        this.sprintStatus = sprintStatus;
        this.storyPoints = storyPoints;
        this.parentIssueKey = parentIssueKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.readModelUpdatedAt = readModelUpdatedAt;
    }

    public UUID issueId() { return issueId; }
    public UUID projectId() { return projectId; }
    public String issueKey() { return issueKey; }
    public String type() { return type; }
    public String title() { return title; }
    public String description() { return description; }
    public UUID statusId() { return statusId; }
    public String statusName() { return statusName; }
    public int statusPosition() { return statusPosition; }
    public String priority() { return priority; }
    public long version() { return version; }
    public UUID assigneeId() { return assigneeId; }
    public String assigneeName() { return assigneeName; }
    public UUID reporterId() { return reporterId; }
    public String reporterName() { return reporterName; }
    public UUID sprintId() { return sprintId; }
    public String sprintName() { return sprintName; }
    public String sprintStatus() { return sprintStatus; }
    public Integer storyPoints() { return storyPoints; }
    public String parentIssueKey() { return parentIssueKey; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
