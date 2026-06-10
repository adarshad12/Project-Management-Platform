package com.dealshare.projectmanagement.infrastructure.persistence.repository;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueJpaRepository extends JpaRepository<IssueEntity, UUID> {

    Optional<IssueEntity> findByProjectIdAndIssueKey(UUID projectId, String issueKey);

    Optional<IssueEntity> findByIssueKey(String issueKey);

    List<IssueEntity> findByProjectIdAndSprintId(UUID projectId, UUID sprintId);

    List<IssueEntity> findBySprintId(UUID sprintId);

    List<IssueEntity> findByProjectIdAndStatusId(UUID projectId, UUID statusId, Pageable pageable);

    long countByProjectIdAndStatusId(UUID projectId, UUID statusId);

    List<IssueEntity> findByParentId(UUID parentId);

    @Query("""
            select issue
            from IssueEntity issue
            join fetch issue.status
            left join fetch issue.assignee
            left join fetch issue.sprint
            where issue.project.id = :projectId
            order by issue.createdAt desc
            """)
    List<IssueEntity> findBoardIssuesByProjectId(@Param("projectId") UUID projectId);

    @Query(value = """
            select coalesce(max(cast(substring(issue_key from '-(\\d+)$') as integer)), 0)
            from issues
            where project_id = :projectId
              and issue_key ~ '-\\d+$'
            """, nativeQuery = true)
    int findMaxIssueNumber(@Param("projectId") UUID projectId);

    @Query(value = """
            select distinct i.*
            from issues i
            left join comments c on c.issue_id = i.id
            where i.search_vector @@ plainto_tsquery('english', :query)
               or c.search_vector @@ plainto_tsquery('english', :query)
               or lower(i.issue_key) = lower(:query)
            order by i.updated_at desc
            limit :limit
            """, nativeQuery = true)
    List<IssueEntity> search(@Param("query") String query, @Param("limit") int limit);

    @Query(value = """
            select distinct i.*
            from issues i
            join workflow_statuses ws on ws.id = i.status_id
            left join users assignee on assignee.id = i.assignee_id
            left join comments c on c.issue_id = i.id
            where (:projectId is null or i.project_id = :projectId)
              and (:status is null or lower(ws.name) = lower(:status))
              and (:assignee is null or lower(assignee.email) = lower(:assignee) or lower(assignee.display_name) = lower(:assignee))
              and (:priority is null or lower(i.priority) = lower(:priority))
              and (:type is null or lower(i.type) = lower(:type))
              and (:textQuery is null or i.search_vector @@ plainto_tsquery('english', :textQuery)
                   or c.search_vector @@ plainto_tsquery('english', :textQuery)
                   or lower(i.issue_key) = lower(:textQuery))
              and (:cursorUpdatedAt is null or i.updated_at < cast(:cursorUpdatedAt as timestamptz))
            order by i.updated_at desc, i.id desc
            limit :limit
            """, nativeQuery = true)
    List<IssueEntity> searchAdvanced(
            @Param("projectId") UUID projectId,
            @Param("textQuery") String textQuery,
            @Param("status") String status,
            @Param("assignee") String assignee,
            @Param("priority") String priority,
            @Param("type") String type,
            @Param("cursorUpdatedAt") String cursorUpdatedAt,
            @Param("limit") int limit
    );
}
