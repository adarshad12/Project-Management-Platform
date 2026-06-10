package com.dealshare.projectmanagement.sprint.api;

import jakarta.validation.constraints.NotBlank;

public record MoveIssueToSprintRequest(@NotBlank String issueId) {
}
