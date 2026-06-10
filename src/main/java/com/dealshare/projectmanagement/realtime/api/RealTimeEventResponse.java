package com.dealshare.projectmanagement.realtime.api;

import java.time.Instant;
import java.util.UUID;

public record RealTimeEventResponse(
        UUID eventId,
        String type,
        UUID projectId,
        String issueId,
        UUID sprintId,
        String payload,
        Instant occurredAt
) {
}
