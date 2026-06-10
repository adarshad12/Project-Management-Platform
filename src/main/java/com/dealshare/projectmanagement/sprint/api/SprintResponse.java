package com.dealshare.projectmanagement.sprint.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SprintResponse(
        UUID id,
        UUID projectId,
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Instant completedAt,
        int completedStoryPoints,
        int carriedOverStoryPoints
) {
}
