package com.dealshare.projectmanagement.collaboration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCommentRequest(
        @NotNull UUID authorId,
        UUID parentCommentId,
        @NotBlank String body
) {
}
