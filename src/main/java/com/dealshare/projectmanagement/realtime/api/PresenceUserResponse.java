package com.dealshare.projectmanagement.realtime.api;

import java.time.Instant;
import java.util.UUID;

public record PresenceUserResponse(
        UUID userId,
        String displayName,
        String sessionId,
        Instant lastSeenAt
) {
}
