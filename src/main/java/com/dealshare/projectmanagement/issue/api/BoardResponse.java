package com.dealshare.projectmanagement.issue.api;

import java.util.List;
import java.util.UUID;

public record BoardResponse(UUID projectId, List<BoardColumnResponse> columns) {
}
