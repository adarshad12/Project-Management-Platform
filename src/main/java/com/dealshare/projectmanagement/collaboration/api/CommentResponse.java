package com.dealshare.projectmanagement.collaboration.api;

import com.dealshare.projectmanagement.issue.api.UserSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String issueId,
        UUID parentCommentId,
        UserSummaryResponse author,
        String body,
        List<UserSummaryResponse> mentions,
        List<CommentResponse> replies,
        Instant createdAt,
        Instant updatedAt
) {
}
