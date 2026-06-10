package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 80)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant deliveredAt;

    @Column(nullable = false)
    private int retryCount;

    private String lastError;

    private Instant nextAttemptAt;

    protected NotificationEntity() {
    }

    public NotificationEntity(
            UUID id,
            UserEntity user,
            String eventType,
            String payload,
            String status,
            Instant createdAt,
            Instant deliveredAt
    ) {
        this.id = id;
        this.user = user;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.deliveredAt = deliveredAt;
        this.retryCount = 0;
    }

    public UUID id() {
        return id;
    }

    public UserEntity user() {
        return user;
    }

    public String eventType() {
        return eventType;
    }

    public String payload() {
        return payload;
    }

    public String status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant deliveredAt() {
        return deliveredAt;
    }

    public int retryCount() {
        return retryCount;
    }

    public String lastError() {
        return lastError;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public void markDelivered(Instant deliveredAt) {
        this.status = "delivered";
        this.deliveredAt = deliveredAt;
        this.lastError = null;
        this.nextAttemptAt = null;
    }

    public void markFailed(String lastError, Instant nextAttemptAt) {
        this.status = "failed";
        this.retryCount++;
        this.lastError = lastError;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markPending() {
        this.status = "pending";
    }
}
