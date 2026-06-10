package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.BoardIssueReadModelEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardIssueReadModelJpaRepository extends JpaRepository<BoardIssueReadModelEntity, UUID> {

    List<BoardIssueReadModelEntity> findByProjectIdOrderByStatusPositionAscCreatedAtDesc(UUID projectId);

    void deleteByIssueId(UUID issueId);
}
