package com.dealshare.projectmanagement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class IssueCustomFieldValueId implements Serializable {

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "field_definition_id")
    private UUID fieldDefinitionId;

    protected IssueCustomFieldValueId() {
    }

    public IssueCustomFieldValueId(UUID issueId, UUID fieldDefinitionId) {
        this.issueId = issueId;
        this.fieldDefinitionId = fieldDefinitionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IssueCustomFieldValueId that)) {
            return false;
        }
        return Objects.equals(issueId, that.issueId) && Objects.equals(fieldDefinitionId, that.fieldDefinitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, fieldDefinitionId);
    }
}
