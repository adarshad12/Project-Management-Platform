package com.dealshare.projectmanagement.sprint.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.common.idempotency.IdempotencyService;
import com.dealshare.projectmanagement.infrastructure.persistence.AdvisoryLockRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ActivityLogEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.DomainEventOutboxEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintCompletionIssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintCompletionIssueId;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintCompletionIssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.issue.api.IssueResponse;
import com.dealshare.projectmanagement.issue.application.BoardReadModelService;
import com.dealshare.projectmanagement.issue.application.IssueMapper;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.dealshare.projectmanagement.security.SecurityContext;
import com.dealshare.projectmanagement.sprint.api.CompleteSprintRequest;
import com.dealshare.projectmanagement.sprint.api.CreateSprintRequest;
import com.dealshare.projectmanagement.sprint.api.SprintCompletionResponse;
import com.dealshare.projectmanagement.sprint.api.SprintResponse;
import com.dealshare.projectmanagement.sprint.api.UpdateSprintRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SprintService {

    private final ProjectJpaRepository projects;
    private final SprintJpaRepository sprints;
    private final IssueJpaRepository issues;
    private final UserJpaRepository users;
    private final SprintCompletionIssueJpaRepository sprintCompletionIssues;
    private final AdvisoryLockRepository advisoryLocks;
    private final ActivityLogJpaRepository activityLog;
    private final DomainEventOutboxJpaRepository outbox;
    private final RealTimeEventPublisher realTimeEvents;
    private final SprintMapper sprintMapper;
    private final IssueMapper issueMapper;
    private final ObjectMapper objectMapper;
    private final SecurityContext securityContext;
    private final BoardReadModelService boardReadModel;
    private final IdempotencyService idempotency;

    public SprintService(
            ProjectJpaRepository projects,
            SprintJpaRepository sprints,
            IssueJpaRepository issues,
            UserJpaRepository users,
            SprintCompletionIssueJpaRepository sprintCompletionIssues,
            AdvisoryLockRepository advisoryLocks,
            ActivityLogJpaRepository activityLog,
            DomainEventOutboxJpaRepository outbox,
            RealTimeEventPublisher realTimeEvents,
            SprintMapper sprintMapper,
            IssueMapper issueMapper,
            ObjectMapper objectMapper,
            SecurityContext securityContext,
            BoardReadModelService boardReadModel,
            IdempotencyService idempotency
    ) {
        this.projects = projects;
        this.sprints = sprints;
        this.issues = issues;
        this.users = users;
        this.sprintCompletionIssues = sprintCompletionIssues;
        this.advisoryLocks = advisoryLocks;
        this.activityLog = activityLog;
        this.outbox = outbox;
        this.realTimeEvents = realTimeEvents;
        this.sprintMapper = sprintMapper;
        this.issueMapper = issueMapper;
        this.objectMapper = objectMapper;
        this.securityContext = securityContext;
        this.boardReadModel = boardReadModel;
        this.idempotency = idempotency;
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> listSprints(UUID projectId) {
        project(projectId);
        return sprints.findByProjectIdOrderByStartDateDescCreatedAtDesc(projectId)
                .stream()
                .map(sprintMapper::toResponse)
                .toList();
    }

    @Transactional
    public SprintResponse createSprint(UUID projectId, CreateSprintRequest request, String idempotencyKey) {
        return idempotency.execute("createSprint:" + projectId, request, idempotencyKey, SprintResponse.class, () -> createSprint(projectId, request));
    }

    private SprintResponse createSprint(UUID projectId, CreateSprintRequest request) {
        ProjectEntity project = project(projectId);
        validateDateRange(request.startDate(), request.endDate());
        Instant now = Instant.now();
        SprintEntity sprint = sprints.save(new SprintEntity(
                UUID.randomUUID(),
                project,
                request.name(),
                request.goal(),
                request.startDate(),
                request.endDate(),
                "planned",
                now,
                now
        ));
        recordSprintEvent(project, sprint, "SprintCreated", Map.of("sprintId", sprint.id(), "name", sprint.name()));
        return sprintMapper.toResponse(sprint);
    }

    @Transactional
    public SprintResponse updateSprint(UUID sprintId, UpdateSprintRequest request, String idempotencyKey) {
        return idempotency.execute("updateSprint:" + sprintId, request, idempotencyKey, SprintResponse.class, () -> updateSprint(sprintId, request));
    }

    private SprintResponse updateSprint(UUID sprintId, UpdateSprintRequest request) {
        SprintEntity sprint = sprint(sprintId);
        ensureNotCompleted(sprint);
        validateDateRange(request.startDate(), request.endDate());
        sprint.update(request.name(), request.goal(), request.startDate(), request.endDate());
        SprintEntity saved = sprints.save(sprint);
        boardReadModel.refreshIssues(issues.findBySprintId(saved.id()));
        recordSprintEvent(project(saved.projectId()), saved, "SprintUpdated", Map.of("sprintId", saved.id()));
        return sprintMapper.toResponse(saved);
    }

    @Transactional
    public void deleteSprint(UUID sprintId) {
        SprintEntity sprint = sprint(sprintId);
        if (!"planned".equals(sprint.status())) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Only planned sprints can be deleted");
        }
        if (!issues.findBySprintId(sprintId).isEmpty()) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Sprint still contains issues");
        }
        sprints.delete(sprint);
    }

    @Transactional
    public SprintResponse startSprint(UUID sprintId, com.dealshare.projectmanagement.sprint.api.StartSprintRequest request, String idempotencyKey) {
        return idempotency.execute("startSprint:" + sprintId, request, idempotencyKey, SprintResponse.class, () -> startSprint(sprintId, request));
    }

    private SprintResponse startSprint(UUID sprintId, com.dealshare.projectmanagement.sprint.api.StartSprintRequest request) {
        advisoryLocks.lockSprint(sprintId);
        SprintEntity sprint = sprint(sprintId);
        if (!"planned".equals(sprint.status())) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Only planned sprints can be started");
        }
        if (sprints.existsByProjectIdAndStatus(sprint.projectId(), "active")) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Project already has an active sprint");
        }
        validateDateRange(request.startDate(), request.endDate());
        sprint.start(request.startDate(), request.endDate());
        SprintEntity saved = sprints.save(sprint);
        boardReadModel.refreshIssues(issues.findBySprintId(saved.id()));
        recordSprintEvent(project(saved.projectId()), saved, "SprintStarted", Map.of("sprintId", saved.id()));
        return sprintMapper.toResponse(saved);
    }

    @Transactional
    public SprintCompletionResponse completeSprint(UUID sprintId, CompleteSprintRequest request, String idempotencyKey) {
        return idempotency.execute("completeSprint:" + sprintId, request, idempotencyKey, SprintCompletionResponse.class, () -> completeSprint(sprintId, request));
    }

    private SprintCompletionResponse completeSprint(UUID sprintId, CompleteSprintRequest request) {
        advisoryLocks.lockSprint(sprintId);
        SprintEntity sprint = sprint(sprintId);
        if (!"active".equals(sprint.status())) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Only active sprints can be completed");
        }

        SprintEntity targetSprint = request.targetSprintId() == null ? null : sprint(request.targetSprintId());
        if (targetSprint != null && !targetSprint.projectId().equals(sprint.projectId())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Carry-over target sprint must belong to the same project");
        }

        Set<String> carryOverIssueIds = new HashSet<>(request.carryOverIssueIds() == null ? List.of() : request.carryOverIssueIds());
        List<IssueEntity> sprintIssues = issues.findBySprintId(sprintId);
        List<IssueEntity> completed = sprintIssues.stream().filter(this::isDone).toList();
        List<IssueEntity> incomplete = sprintIssues.stream().filter(issue -> !isDone(issue)).toList();
        List<IssueEntity> carriedOver = incomplete.stream()
                .filter(issue -> carryOverIssueIds.contains(issue.issueKey()) || carryOverIssueIds.contains(issue.id().toString()))
                .toList();
        List<IssueEntity> movedToBacklog = incomplete.stream()
                .filter(issue -> !carriedOver.contains(issue))
                .toList();

        if (!carriedOver.isEmpty() && targetSprint == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "targetSprintId is required when carrying over issues");
        }

        carriedOver.forEach(issue -> issue.moveToSprint(targetSprint));
        movedToBacklog.forEach(IssueEntity::moveToBacklog);
        issues.saveAll(incomplete);

        Instant now = Instant.now();
        completed.forEach(issue -> sprintCompletionIssues.save(new SprintCompletionIssueEntity(
                new SprintCompletionIssueId(sprint.id(), issue.id()),
                sprint,
                issue,
                "completed",
                null,
                now
        )));
        carriedOver.forEach(issue -> sprintCompletionIssues.save(new SprintCompletionIssueEntity(
                new SprintCompletionIssueId(sprint.id(), issue.id()),
                sprint,
                issue,
                "carried_over",
                targetSprint,
                now
        )));
        movedToBacklog.forEach(issue -> sprintCompletionIssues.save(new SprintCompletionIssueEntity(
                new SprintCompletionIssueId(sprint.id(), issue.id()),
                sprint,
                issue,
                "moved_to_backlog",
                null,
                now
        )));

        int completedPoints = sumStoryPoints(completed);
        int carriedPoints = sumStoryPoints(carriedOver) + sumStoryPoints(movedToBacklog);
        sprint.complete(completedPoints, carriedPoints);
        SprintEntity saved = sprints.save(sprint);
        boardReadModel.refreshIssues(sprintIssues);

        recordSprintEvent(project(saved.projectId()), saved, "SprintCompleted", Map.of(
                "sprintId", saved.id(),
                "completedStoryPoints", completedPoints,
                "carriedOverStoryPoints", carriedPoints
        ));

        return new SprintCompletionResponse(
                sprintMapper.toResponse(saved),
                toIssueResponses(completed),
                toIssueResponses(carriedOver),
                toIssueResponses(movedToBacklog)
        );
    }

    @Transactional
    public IssueResponse moveIssueToSprint(UUID sprintId, String issueId, String idempotencyKey) {
        return idempotency.execute("moveIssueToSprint:" + sprintId, issueId, idempotencyKey, IssueResponse.class, () -> moveIssueToSprint(sprintId, issueId));
    }

    private IssueResponse moveIssueToSprint(UUID sprintId, String issueId) {
        SprintEntity sprint = sprint(sprintId);
        if ("completed".equals(sprint.status())) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Cannot move issues into a completed sprint");
        }
        IssueEntity issue = issue(issueId);
        if (!issue.projectId().equals(sprint.projectId())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Issue and sprint must belong to the same project");
        }
        issue.moveToSprint(sprint);
        IssueEntity saved = issues.saveAndFlush(issue);
        boardReadModel.refreshIssue(saved);
        recordSprintEvent(project(sprint.projectId()), sprint, "SprintUpdated", Map.of("sprintId", sprint.id(), "issueMovedIn", saved.issueKey()));
        return issueMapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse moveIssueToBacklog(UUID sprintId, String issueId) {
        sprint(sprintId);
        IssueEntity issue = issue(issueId);
        if (issue.sprint() == null || !issue.sprint().id().equals(sprintId)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Issue is not in this sprint");
        }
        issue.moveToBacklog();
        IssueEntity saved = issues.saveAndFlush(issue);
        boardReadModel.refreshIssue(saved);
        return issueMapper.toResponse(saved);
    }

    private List<IssueResponse> toIssueResponses(List<IssueEntity> issues) {
        return issues.stream().map(issueMapper::toResponse).toList();
    }

    private boolean isDone(IssueEntity issue) {
        return "done".equals(issue.status().category());
    }

    private int sumStoryPoints(List<IssueEntity> issues) {
        return issues.stream().map(IssueEntity::storyPoints).mapToInt(points -> points == null ? 0 : points).sum();
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Sprint startDate must be on or before endDate");
        }
    }

    private void ensureNotCompleted(SprintEntity sprint) {
        if ("completed".equals(sprint.status())) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Completed sprints cannot be modified");
        }
    }

    private ProjectEntity project(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project not found"));
    }

    private SprintEntity sprint(UUID sprintId) {
        return sprints.findById(sprintId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Sprint not found"));
    }

    private IssueEntity issue(String issueId) {
        try {
            return issues.findById(UUID.fromString(issueId))
                    .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Issue not found"));
        } catch (IllegalArgumentException ignored) {
            return issues.findByIssueKey(issueId)
                    .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Issue not found"));
        }
    }

    private void recordSprintEvent(ProjectEntity project, SprintEntity sprint, String eventType, Map<String, Object> payload) {
        String payloadJson = writeJson(payload);
        Instant now = Instant.now();
        activityLog.save(new ActivityLogEntity(UUID.randomUUID(), project, null, currentActor(), eventType, payloadJson, now));
        outbox.save(new DomainEventOutboxEntity(UUID.randomUUID(), "Sprint", sprint.id(), eventType, payloadJson, now));
        realTimeEvents.publishSprintEvent(project.id(), sprint.id(), eventType, payload);
    }

    private com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity currentActor() {
        try {
            return users.findById(securityContext.currentUserId()).orElse(null);
        } catch (DomainException ignored) {
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON");
        }
    }
}
