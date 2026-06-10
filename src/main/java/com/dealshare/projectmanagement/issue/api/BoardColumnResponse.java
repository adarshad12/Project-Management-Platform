package com.dealshare.projectmanagement.issue.api;

import java.util.List;
import java.util.UUID;

public record BoardColumnResponse(UUID statusId, String name, int position, List<IssueResponse> issues) {
}
