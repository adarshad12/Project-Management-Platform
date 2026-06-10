package com.dealshare.projectmanagement.realtime.api;

import java.util.UUID;

public record PresenceRequest(
        UUID projectId,
        String issueId,
        UUID userId,
        String displayName
) {
}
