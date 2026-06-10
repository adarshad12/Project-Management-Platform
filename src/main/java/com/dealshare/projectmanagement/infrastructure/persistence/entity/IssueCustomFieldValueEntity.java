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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "issue_custom_field_values")
public class IssueCustomFieldValueEntity {

    @EmbeddedId
    private IssueCustomFieldValueId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("issueId")
    @JoinColumn(name = "issue_id", nullable = false)
    private IssueEntity issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("fieldDefinitionId")
    @JoinColumn(name = "field_definition_id", nullable = false)
    private CustomFieldDefinitionEntity fieldDefinition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String value;

    @Column(nullable = false)
    private Instant updatedAt;

    protected IssueCustomFieldValueEntity() {
    }

    public IssueCustomFieldValueEntity(
            IssueCustomFieldValueId id,
            IssueEntity issue,
            CustomFieldDefinitionEntity fieldDefinition,
            String value,
            Instant updatedAt
    ) {
        this.id = id;
        this.issue = issue;
        this.fieldDefinition = fieldDefinition;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public IssueEntity issue() {
        return issue;
    }

    public CustomFieldDefinitionEntity fieldDefinition() {
        return fieldDefinition;
    }

    public String value() {
        return value;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void updateValue(String value) {
        this.value = value;
        this.updatedAt = Instant.now();
    }
}
