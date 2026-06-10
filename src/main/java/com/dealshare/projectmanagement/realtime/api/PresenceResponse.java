package com.dealshare.projectmanagement.realtime.api;

import java.util.List;
import java.util.UUID;

public record PresenceResponse(
        UUID projectId,
        String issueId,
        List<PresenceUserResponse> users
) {
}
