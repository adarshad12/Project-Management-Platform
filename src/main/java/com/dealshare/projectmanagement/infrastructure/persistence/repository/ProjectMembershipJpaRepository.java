package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMembershipJpaRepository extends JpaRepository<ProjectMembershipEntity, ProjectMembershipId> {

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMembershipEntity> findByUserId(UUID userId);

    List<ProjectMembershipEntity> findByProjectIdAndRole(UUID projectId, String role);
}
