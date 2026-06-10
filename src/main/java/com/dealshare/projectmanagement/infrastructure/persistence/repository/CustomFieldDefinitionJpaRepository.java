package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.CustomFieldDefinitionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldDefinitionJpaRepository extends JpaRepository<CustomFieldDefinitionEntity, UUID> {

    List<CustomFieldDefinitionEntity> findByProjectId(UUID projectId);

    Optional<CustomFieldDefinitionEntity> findByProjectIdAndFieldKey(UUID projectId, String fieldKey);
}
