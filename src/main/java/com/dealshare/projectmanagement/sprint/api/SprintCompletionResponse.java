package com.dealshare.projectmanagement.sprint.api;

import com.dealshare.projectmanagement.issue.api.IssueResponse;
import java.util.List;

public record SprintCompletionResponse(
        SprintResponse sprint,
        List<IssueResponse> completedIssues,
        List<IssueResponse> carriedOverIssues,
        List<IssueResponse> movedToBacklogIssues
) {
}
