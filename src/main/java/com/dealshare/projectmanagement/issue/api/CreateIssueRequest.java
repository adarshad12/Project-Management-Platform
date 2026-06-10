package com.dealshare.projectmanagement.issue.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record CreateIssueRequest(
        @NotBlank String type,
        @NotBlank String title,
        String description,
        String status,
        String priority,
        UUID assigneeId,
        @NotNull UUID reporterId,
        UUID sprintId,
        @PositiveOrZero Integer storyPoints,
        String parentId
) {
}
