package com.dealshare.projectmanagement.sprint.api;

import java.time.LocalDate;

public record UpdateSprintRequest(
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate
) {
}
