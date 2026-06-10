package com.dealshare.projectmanagement.collaboration.api;

import com.dealshare.projectmanagement.issue.api.UserSummaryResponse;
import java.time.Instant;

public record WatcherResponse(UserSummaryResponse user, Instant createdAt) {
}
