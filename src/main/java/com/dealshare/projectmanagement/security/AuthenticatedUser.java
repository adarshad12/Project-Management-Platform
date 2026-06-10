package com.dealshare.projectmanagement.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String displayName) {
}
