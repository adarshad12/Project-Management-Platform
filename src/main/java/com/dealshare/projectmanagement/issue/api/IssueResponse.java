package com.dealshare.projectmanagement.issue.api;

import java.time.Instant;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        String issueId,
        UUID projectId,
        String type,
        String title,
        String description,
        String status,
        String priority,
        long version,
        UserSummaryResponse assignee,
        UserSummaryResponse reporter,
        SprintSummaryResponse sprint,
        Integer storyPoints,
        String parentId,
        Instant createdAt,
        Instant updatedAt
) {
}
