package com.dealshare.projectmanagement.issue.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransitionIssueRequest(
        @NotNull Long version,
        @NotBlank String toStatus
) {
}
