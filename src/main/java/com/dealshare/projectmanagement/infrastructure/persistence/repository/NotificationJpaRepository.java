package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.NotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status, Pageable pageable);

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            select notification
            from NotificationEntity notification
            where notification.status in ('pending', 'failed')
              and (notification.nextAttemptAt is null or notification.nextAttemptAt <= :now)
            order by notification.createdAt asc
            """)
    List<NotificationEntity> findDueForDelivery(@Param("now") Instant now, Pageable pageable);
}
