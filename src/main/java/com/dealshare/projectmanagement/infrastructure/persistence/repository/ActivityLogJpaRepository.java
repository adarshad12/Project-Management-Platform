package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.ActivityLogEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogJpaRepository extends JpaRepository<ActivityLogEntity, UUID> {

    List<ActivityLogEntity> findByProjectIdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(UUID projectId, Instant cursor, Pageable pageable);

    List<ActivityLogEntity> findByProjectIdAndEventTypeAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
            UUID projectId,
            String eventType,
            Instant cursor,
            Pageable pageable
    );

    @Query("""
            select activity
            from ActivityLogEntity activity
            where activity.project.id = :projectId
              and activity.createdAt < :cursor
              and (:actorId is null or activity.actor.id = :actorId)
              and (:issueId is null or activity.issue.id = :issueId)
              and (:eventType is null or activity.eventType = :eventType)
              and activity.createdAt >= :from
              and activity.createdAt <= :to
            order by activity.createdAt desc, activity.id desc
            """)
    List<ActivityLogEntity> findProjectActivity(
            @Param("projectId") UUID projectId,
            @Param("cursor") Instant cursor,
            @Param("actorId") UUID actorId,
            @Param("issueId") UUID issueId,
            @Param("eventType") String eventType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
