package com.dealshare.projectmanagement.realtime.api;

import java.util.UUID;

public record ReplayRequest(
        UUID projectId,
        String issueId,
        UUID lastSeenEventId
) {
}
