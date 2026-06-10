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
@Table(name = "custom_field_definitions")
public class CustomFieldDefinitionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 80)
    private String fieldKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 32)
    private String fieldType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String options;

    @Column(nullable = false)
    private boolean isRequired;

    @Column(nullable = false)
    private Instant createdAt;

    protected CustomFieldDefinitionEntity() {
    }

    public CustomFieldDefinitionEntity(
            UUID id,
            ProjectEntity project,
            String fieldKey,
            String name,
            String fieldType,
            String options,
            boolean isRequired,
            Instant createdAt
    ) {
        this.id = id;
        this.project = project;
        this.fieldKey = fieldKey;
        this.name = name;
        this.fieldType = fieldType;
        this.options = options;
        this.isRequired = isRequired;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID projectId() {
        return project.id();
    }

    public String fieldKey() {
        return fieldKey;
    }

    public String name() {
        return name;
    }

    public String fieldType() {
        return fieldType;
    }

    public String options() {
        return options;
    }

    public boolean required() {
        return isRequired;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void update(String name, String fieldType, String options, Boolean required) {
        if (name != null) {
            this.name = name;
        }
        if (fieldType != null) {
            this.fieldType = fieldType;
        }
        if (options != null) {
            this.options = options;
        }
        if (required != null) {
            this.isRequired = required;
        }
    }
}
