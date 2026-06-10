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
@Table(name = "issue_watchers")
public class IssueWatcherEntity {

    @EmbeddedId
    private IssueWatcherId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("issueId")
    @JoinColumn(name = "issue_id", nullable = false)
    private IssueEntity issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private Instant createdAt;

    protected IssueWatcherEntity() {
    }

    public IssueWatcherEntity(IssueWatcherId id, IssueEntity issue, UserEntity user, Instant createdAt) {
        this.id = id;
        this.issue = issue;
        this.user = user;
        this.createdAt = createdAt;
    }

    public IssueWatcherId id() {
        return id;
    }

    public IssueEntity issue() {
        return issue;
    }

    public UserEntity user() {
        return user;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
