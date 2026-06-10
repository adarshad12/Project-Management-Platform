package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "workflow_statuses")
public class WorkflowStatusEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false)
    private int position;

    private Integer wipLimit;

    protected WorkflowStatusEntity() {
    }

    public WorkflowStatusEntity(UUID id, ProjectEntity project, String name, String category, int position, Integer wipLimit) {
        this.id = id;
        this.project = project;
        this.name = name;
        this.category = category;
        this.position = position;
        this.wipLimit = wipLimit;
    }

    public UUID id() {
        return id;
    }

    public UUID projectId() {
        return project.id();
    }

    public String name() {
        return name;
    }

    public String category() {
        return category;
    }

    public int position() {
        return position;
    }

    public Integer wipLimit() {
        return wipLimit;
    }
}
