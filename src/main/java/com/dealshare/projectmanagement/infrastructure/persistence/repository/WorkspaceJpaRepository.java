package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkspaceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceEntity, UUID> {
}
