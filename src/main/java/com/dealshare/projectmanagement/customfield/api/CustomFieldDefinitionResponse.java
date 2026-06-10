package com.dealshare.projectmanagement.customfield.api;

import java.time.Instant;
import java.util.UUID;

public record CustomFieldDefinitionResponse(
        UUID id,
        UUID projectId,
        String fieldKey,
        String name,
        String fieldType,
        String options,
        boolean required,
        Instant createdAt
) {
}
