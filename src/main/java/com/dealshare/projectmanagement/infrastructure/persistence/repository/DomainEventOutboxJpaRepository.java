package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.DomainEventOutboxEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainEventOutboxJpaRepository extends JpaRepository<DomainEventOutboxEntity, UUID> {

    List<DomainEventOutboxEntity> findByProcessedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
}
