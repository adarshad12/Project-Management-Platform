package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintCompletionIssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintCompletionIssueId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintCompletionIssueJpaRepository extends JpaRepository<SprintCompletionIssueEntity, SprintCompletionIssueId> {

    List<SprintCompletionIssueEntity> findBySprintId(UUID sprintId);
}
