package com.dealshare.projectmanagement.collaboration.api;

import java.util.List;

public record ActivityFeedResponse(
        List<ActivityResponse> items,
        String nextCursor
) {
}
