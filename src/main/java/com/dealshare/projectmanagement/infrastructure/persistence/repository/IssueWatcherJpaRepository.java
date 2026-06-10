package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueWatcherEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueWatcherId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueWatcherJpaRepository extends JpaRepository<IssueWatcherEntity, IssueWatcherId> {

    List<IssueWatcherEntity> findByIssueId(UUID issueId);

    boolean existsByIssueIdAndUserId(UUID issueId, UUID userId);

    void deleteByIssueIdAndUserId(UUID issueId, UUID userId);
}
