package com.dealshare.projectmanagement.issue.api;

import java.util.List;

public record SearchResponse(
        List<IssueResponse> items,
        String nextCursor
) {
}
