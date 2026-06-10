package com.dealshare.projectmanagement.issue.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record UpdateIssueRequest(
        @NotNull Long version,
        String title,
        String description,
        String priority,
        UUID assigneeId,
        UUID sprintId,
        @PositiveOrZero Integer storyPoints
) {
}
