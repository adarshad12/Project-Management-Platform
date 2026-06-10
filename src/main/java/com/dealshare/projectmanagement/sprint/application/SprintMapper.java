package com.dealshare.projectmanagement.sprint.application;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.sprint.api.SprintResponse;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

    public SprintResponse toResponse(SprintEntity sprint) {
        return new SprintResponse(
                sprint.id(),
                sprint.projectId(),
                sprint.name(),
                sprint.goal(),
                sprint.startDate(),
                sprint.endDate(),
                sprint.status(),
                sprint.completedAt(),
                sprint.completedStoryPoints(),
                sprint.carriedOverStoryPoints()
        );
    }
}
