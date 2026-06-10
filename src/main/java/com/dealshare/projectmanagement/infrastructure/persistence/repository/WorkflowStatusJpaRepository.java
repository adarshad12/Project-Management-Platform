package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowStatusEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowStatusJpaRepository extends JpaRepository<WorkflowStatusEntity, UUID> {

    List<WorkflowStatusEntity> findByProjectIdOrderByPositionAsc(UUID projectId);

    Optional<WorkflowStatusEntity> findByProjectIdAndName(UUID projectId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select status from WorkflowStatusEntity status where status.id = :statusId")
    Optional<WorkflowStatusEntity> lockById(@Param("statusId") UUID statusId);
}
