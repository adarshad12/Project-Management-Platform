package com.dealshare.projectmanagement.issue.api;

import java.util.UUID;

public record SprintSummaryResponse(UUID sprintId, String name, String status) {
}
