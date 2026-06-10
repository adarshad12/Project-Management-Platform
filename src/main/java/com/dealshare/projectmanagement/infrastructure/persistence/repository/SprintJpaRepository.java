package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintJpaRepository extends JpaRepository<SprintEntity, UUID> {

    List<SprintEntity> findByProjectIdOrderByStartDateDescCreatedAtDesc(UUID projectId);

    Optional<SprintEntity> findFirstByProjectIdAndStatus(UUID projectId, String status);

    boolean existsByProjectIdAndStatus(UUID projectId, String status);
}
