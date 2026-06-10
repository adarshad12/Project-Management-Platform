package com.dealshare.projectmanagement.collaboration.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String eventType,
        String payload,
        String status,
        Instant createdAt,
        Instant deliveredAt
) {
}
