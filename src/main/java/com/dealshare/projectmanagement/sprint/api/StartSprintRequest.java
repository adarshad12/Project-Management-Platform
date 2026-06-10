package com.dealshare.projectmanagement.sprint.api;

import java.time.LocalDate;

public record StartSprintRequest(LocalDate startDate, LocalDate endDate) {
}
