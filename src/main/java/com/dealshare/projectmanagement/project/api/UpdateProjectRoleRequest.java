package com.dealshare.projectmanagement.project.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectRoleRequest(@NotBlank String role) {
}
