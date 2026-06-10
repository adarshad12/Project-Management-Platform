package com.dealshare.projectmanagement.sprint.api;

import java.util.List;
import java.util.UUID;

public record CompleteSprintRequest(
        UUID targetSprintId,
        List<String> carryOverIssueIds
) {
}
