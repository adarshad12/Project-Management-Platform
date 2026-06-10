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
@Table(name = "project_memberships")
public class ProjectMembershipEntity {

    @EmbeddedId
    private ProjectMembershipId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("projectId")
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(nullable = false)
    private Instant createdAt;

    protected ProjectMembershipEntity() {
    }

    public ProjectMembershipEntity(ProjectMembershipId id, ProjectEntity project, UserEntity user, String role, Instant createdAt) {
        this.id = id;
        this.project = project;
        this.user = user;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UserEntity user() {
        return user;
    }

    public String role() {
        return role;
    }

    public ProjectEntity project() {
        return project;
    }

    public void changeRole(String role) {
        this.role = role;
    }
}
