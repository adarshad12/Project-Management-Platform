package com.dealshare.projectmanagement.issue.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.BoardIssueReadModelEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.BoardIssueReadModelJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkflowStatusJpaRepository;
import com.dealshare.projectmanagement.issue.api.BoardColumnResponse;
import com.dealshare.projectmanagement.issue.api.BoardResponse;
import com.dealshare.projectmanagement.issue.api.IssueResponse;
import com.dealshare.projectmanagement.issue.api.SprintSummaryResponse;
import com.dealshare.projectmanagement.issue.api.UserSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardReadModelService {

    private static final Duration BOARD_CACHE_TTL = Duration.ofSeconds(30);
    private static final String BOARD_CACHE_KEY = "board:project:%s";

    private final BoardIssueReadModelJpaRepository readModel;
    private final IssueJpaRepository issues;
    private final WorkflowStatusJpaRepository statuses;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Timer boardReadTimer;
    private final Timer readModelRefreshTimer;

    public BoardReadModelService(
            BoardIssueReadModelJpaRepository readModel,
            IssueJpaRepository issues,
            WorkflowStatusJpaRepository statuses,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.readModel = readModel;
        this.issues = issues;
        this.statuses = statuses;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.boardReadTimer = Timer.builder("board.read.latency")
                .description("Board read latency from Redis/read model")
                .register(meterRegistry);
        this.readModelRefreshTimer = Timer.builder("board.read_model.refresh.latency")
                .description("Board read model refresh latency")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public BoardResponse board(UUID projectId) {
        return boardReadTimer.record(() -> cachedBoard(projectId));
    }

    @Transactional
    public void refreshIssue(IssueEntity issue) {
        readModelRefreshTimer.record(() -> {
            readModel.save(toReadModel(issue));
            evictBoard(issue.projectId());
        });
    }

    @Transactional
    public void refreshIssues(List<IssueEntity> changedIssues) {
        if (changedIssues.isEmpty()) {
            return;
        }
        readModelRefreshTimer.record(() -> {
            readModel.saveAll(changedIssues.stream().map(this::toReadModel).toList());
            changedIssues.stream().map(IssueEntity::projectId).distinct().forEach(this::evictBoard);
        });
    }

    @Transactional
    public void rebuildProject(UUID projectId) {
        readModelRefreshTimer.record(() -> {
            readModel.saveAll(issues.findBoardIssuesByProjectId(projectId).stream().map(this::toReadModel).toList());
            evictBoard(projectId);
        });
    }

    public void evictBoard(UUID projectId) {
        try {
            redis.delete(BOARD_CACHE_KEY.formatted(projectId));
        } catch (RuntimeException ignored) {
            // Cache invalidation is best-effort; DB read model remains authoritative.
        }
    }

    private BoardResponse cachedBoard(UUID projectId) {
        String key = BOARD_CACHE_KEY.formatted(projectId);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, BoardResponse.class);
            }
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Fall through to DB read-model query.
        }

        BoardResponse board = boardFromReadModel(projectId);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(board), BOARD_CACHE_TTL);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Cache write is best-effort.
        }
        return board;
    }

    private BoardResponse boardFromReadModel(UUID projectId) {
        List<BoardIssueReadModelEntity> rows = readModel.findByProjectIdOrderByStatusPositionAscCreatedAtDesc(projectId);
        if (rows.isEmpty()) {
            return boardFromCanonicalIssues(projectId);
        }

        Map<UUID, List<IssueResponse>> issuesByStatus = rows.stream()
                .collect(Collectors.groupingBy(
                        BoardIssueReadModelEntity::statusId,
                        Collectors.mapping(this::toIssueResponse, Collectors.toList())
                ));

        List<BoardColumnResponse> columns = statuses.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(status -> new BoardColumnResponse(
                        status.id(),
                        status.name(),
                        status.position(),
                        issuesByStatus.getOrDefault(status.id(), List.of())
                ))
                .toList();
        return new BoardResponse(projectId, columns);
    }

    private BoardResponse boardFromCanonicalIssues(UUID projectId) {
        Map<UUID, List<IssueResponse>> issuesByStatus = issues.findBoardIssuesByProjectId(projectId)
                .stream()
                .map(issue -> Map.entry(issue.status().id(), toIssueResponse(issue)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<BoardColumnResponse> columns = statuses.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(status -> new BoardColumnResponse(
                        status.id(),
                        status.name(),
                        status.position(),
                        issuesByStatus.getOrDefault(status.id(), List.of())
                ))
                .toList();
        return new BoardResponse(projectId, columns);
    }

    private BoardIssueReadModelEntity toReadModel(IssueEntity issue) {
        UserEntity assignee = issue.assignee();
        UserEntity reporter = issue.reporter();
        SprintEntity sprint = issue.sprint();
        return new BoardIssueReadModelEntity(
                issue.id(),
                issue.projectId(),
                issue.issueKey(),
                issue.type(),
                issue.title(),
                issue.description(),
                issue.status().id(),
                issue.status().name(),
                issue.status().position(),
                issue.priority(),
                issue.version(),
                assignee == null ? null : assignee.id(),
                assignee == null ? null : assignee.displayName(),
                reporter.id(),
                reporter.displayName(),
                sprint == null ? null : sprint.id(),
                sprint == null ? null : sprint.name(),
                sprint == null ? null : sprint.status(),
                issue.storyPoints(),
                issue.parent() == null ? null : issue.parent().issueKey(),
                issue.createdAt(),
                issue.updatedAt(),
                Instant.now()
        );
    }

    private IssueResponse toIssueResponse(BoardIssueReadModelEntity row) {
        return new IssueResponse(
                row.issueId(),
                row.issueKey(),
                row.projectId(),
                row.type(),
                row.title(),
                row.description(),
                row.statusName(),
                row.priority(),
                row.version(),
                row.assigneeId() == null ? null : new UserSummaryResponse(row.assigneeId(), row.assigneeName()),
                new UserSummaryResponse(row.reporterId(), row.reporterName()),
                row.sprintId() == null ? null : new SprintSummaryResponse(row.sprintId(), row.sprintName(), row.sprintStatus()),
                row.storyPoints(),
                row.parentIssueKey(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private IssueResponse toIssueResponse(IssueEntity issue) {
        UserEntity assignee = issue.assignee();
        UserEntity reporter = issue.reporter();
        SprintEntity sprint = issue.sprint();
        return new IssueResponse(
                issue.id(),
                issue.issueKey(),
                issue.projectId(),
                issue.type(),
                issue.title(),
                issue.description(),
                issue.status().name(),
                issue.priority(),
                issue.version(),
                assignee == null ? null : new UserSummaryResponse(assignee.id(), assignee.displayName()),
                new UserSummaryResponse(reporter.id(), reporter.displayName()),
                sprint == null ? null : new SprintSummaryResponse(sprint.id(), sprint.name(), sprint.status()),
                issue.storyPoints(),
                issue.parent() == null ? null : issue.parent().issueKey(),
                issue.createdAt(),
                issue.updatedAt()
        );
    }
}
