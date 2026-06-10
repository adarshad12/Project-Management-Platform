package com.dealshare.projectmanagement.issue.api;

import java.util.UUID;

public record UserSummaryResponse(UUID userId, String displayName) {
}
