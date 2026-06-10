package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 160)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String responseBody;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    protected IdempotencyKeyEntity() {
    }

    public IdempotencyKeyEntity(
            UUID id,
            String idempotencyKey,
            String requestFingerprint,
            Integer responseStatus,
            String responseBody,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String requestFingerprint() {
        return requestFingerprint;
    }

    public Integer responseStatus() {
        return responseStatus;
    }

    public String responseBody() {
        return responseBody;
    }

    public boolean hasStoredResponse() {
        return responseStatus != null && responseBody != null;
    }
}
