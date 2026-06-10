package com.dealshare.projectmanagement.collaboration.api;

import com.dealshare.projectmanagement.issue.api.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID projectId,
        String issueId,
        UserSummaryResponse actor,
        String eventType,
        String payload,
        Instant createdAt
) {
}
