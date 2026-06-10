package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueCustomFieldValueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueCustomFieldValueId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueCustomFieldValueJpaRepository extends JpaRepository<IssueCustomFieldValueEntity, IssueCustomFieldValueId> {

    List<IssueCustomFieldValueEntity> findByIssueId(UUID issueId);
}
