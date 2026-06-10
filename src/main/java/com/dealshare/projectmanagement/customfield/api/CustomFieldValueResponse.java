package com.dealshare.projectmanagement.customfield.api;

import java.time.Instant;
import java.util.UUID;

public record CustomFieldValueResponse(
        String issueId,
        UUID fieldDefinitionId,
        String fieldKey,
        String name,
        String fieldType,
        String value,
        Instant updatedAt
) {
}
