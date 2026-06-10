package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowTransitionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionJpaRepository extends JpaRepository<WorkflowTransitionEntity, UUID> {

    List<WorkflowTransitionEntity> findByProjectIdAndFromStatusId(UUID projectId, UUID fromStatusId);

    Optional<WorkflowTransitionEntity> findByProjectIdAndFromStatusIdAndToStatusId(UUID projectId, UUID fromStatusId, UUID toStatusId);
}
