package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.CommentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByIssueIdAndParentCommentIsNullOrderByCreatedAtAsc(UUID issueId, Pageable pageable);

    List<CommentEntity> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

    List<CommentEntity> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);
}
