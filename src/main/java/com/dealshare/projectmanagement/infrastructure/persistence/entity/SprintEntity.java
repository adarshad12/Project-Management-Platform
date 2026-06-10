package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sprints")
public class SprintEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 160)
    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private int completedStoryPoints;

    @Column(nullable = false)
    private int carriedOverStoryPoints;

    protected SprintEntity() {
    }

    public SprintEntity(
            UUID id,
            ProjectEntity project,
            String name,
            String goal,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.project = project;
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public UUID projectId() {
        return project.id();
    }

    public String goal() {
        return goal;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public String status() {
        return status;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public int completedStoryPoints() {
        return completedStoryPoints;
    }

    public int carriedOverStoryPoints() {
        return carriedOverStoryPoints;
    }

    public void update(String name, String goal, LocalDate startDate, LocalDate endDate) {
        if (name != null) {
            this.name = name;
        }
        if (goal != null) {
            this.goal = goal;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        this.updatedAt = Instant.now();
    }

    public void start(LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        this.status = "active";
        this.updatedAt = Instant.now();
    }

    public void complete(int completedStoryPoints, int carriedOverStoryPoints) {
        this.status = "completed";
        this.completedStoryPoints = completedStoryPoints;
        this.carriedOverStoryPoints = carriedOverStoryPoints;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }
}
