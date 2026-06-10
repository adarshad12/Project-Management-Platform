package com.dealshare.projectmanagement.project.api;

import java.util.UUID;

public record ProjectMembershipResponse(
        UUID projectId,
        UUID userId,
        String role
) {
}
