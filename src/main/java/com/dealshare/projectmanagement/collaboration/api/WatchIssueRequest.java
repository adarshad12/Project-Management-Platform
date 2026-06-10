package com.dealshare.projectmanagement.collaboration.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WatchIssueRequest(@NotNull UUID userId) {
}
