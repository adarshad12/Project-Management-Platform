package com.dealshare.projectmanagement.security.api;

import java.util.UUID;

public record TokenResponse(
        String tokenType,
        String accessToken,
        UUID userId,
        String email,
        String displayName
) {
}
