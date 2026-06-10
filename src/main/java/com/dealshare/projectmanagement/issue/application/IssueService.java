package com.dealshare.projectmanagement.issue.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.collaboration.application.CollaborationService;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ActivityLogEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.DomainEventOutboxEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowStatusEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowTransitionEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectMembershipJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkflowStatusJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkflowTransitionJpaRepository;
import com.dealshare.projectmanagement.issue.api.BoardColumnResponse;
import com.dealshare.projectmanagement.issue.api.BoardResponse;
import com.dealshare.projectmanagement.issue.api.CreateIssueRequest;
import com.dealshare.projectmanagement.issue.api.IssueResponse;
import com.dealshare.projectmanagement.issue.api.SearchResponse;
import com.dealshare.projectmanagement.issue.api.TransitionIssueRequest;
import com.dealshare.projectmanagement.issue.api.UpdateIssueRequest;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {

    private static final Pattern STRUCTURED_FILTER = Pattern.compile(
            "(?i)\\b(status|assignee|priority|type|projectId)\\s*=\\s*\"([^\"]+)\"|\\b(status|assignee|priority|type|projectId)\\s*=\\s*([^\\s]+)"
    );

    private final ProjectJpaRepository projects;
    private final UserJpaRepository users;
    private final SprintJpaRepository sprints;
    private final IssueJpaRepository issues;
    private final WorkflowStatusJpaRepository statuses;
    private final WorkflowTransitionJpaRepository transitions;
    private final ProjectMembershipJpaRepository memberships;
    private final ActivityLogJpaRepository activityLog;
    private final DomainEventOutboxJpaRepository outbox;
    private final IdempotencyKeyJpaRepository idempotencyKeys;
    private final CollaborationService collaboration;
    private final RealTimeEventPublisher realTimeEvents;
    private final BoardReadModelService boardReadModel;
    private final IssueMapper mapper;
    private final ObjectMapper objectMapper;

    public IssueService(
            ProjectJpaRepository projects,
            UserJpaRepository users,
            SprintJpaRepository sprints,
            IssueJpaRepository issues,
            WorkflowStatusJpaRepository statuses,
            WorkflowTransitionJpaRepository transitions,
            ProjectMembershipJpaRepository memberships,
            ActivityLogJpaRepository activityLog,
            DomainEventOutboxJpaRepository outbox,
            IdempotencyKeyJpaRepository idempotencyKeys,
            CollaborationService collaboration,
            RealTimeEventPublisher realTimeEvents,
            BoardReadModelService boardReadModel,
            IssueMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.projects = projects;
        this.users = users;
        this.sprints = sprints;
        this.issues = issues;
        this.statuses = statuses;
        this.transitions = transitions;
        this.memberships = memberships;
        this.activityLog = activityLog;
        this.outbox = outbox;
        this.idempotencyKeys = idempotencyKeys;
        this.collaboration = collaboration;
        this.realTimeEvents = realTimeEvents;
        this.boardReadModel = boardReadModel;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IssueResponse createIssue(UUID projectId, CreateIssueRequest request, String idempotencyKey) {
        return withIdempotency("createIssue:" + projectId, request, idempotencyKey, IssueResponse.class, () -> {
            ProjectEntity project = project(projectId);
            UserEntity reporter = user(request.reporterId());
            UserEntity assignee = request.assigneeId() == null ? null : user(request.assigneeId());
            SprintEntity sprint = request.sprintId() == null ? null : sprint(request.sprintId());
            IssueEntity parent = request.parentId() == null ? null : issue(request.parentId());
            WorkflowStatusEntity status = request.status() == null
                    ? firstStatus(projectId)
                    : statusByName(projectId, request.status());

            String issueKey = project.key() + "-" + (issues.findMaxIssueNumber(projectId) + 1);
            Instant now = Instant.now();
            IssueEntity issue = issues.save(new IssueEntity(
                    UUID.randomUUID(),
                    project,
                    issueKey,
                    normalizeIssueType(request.type()),
                    request.title(),
                    request.description(),
                    status,
                    normalizePriority(request.priority()),
                    assignee,
                    reporter,
                    sprint,
                    request.storyPoints(),
                    parent,
                    now,
                    now
            ));

            recordEvent(project, issue, reporter, "IssueCreated", Map.of("issueKey", issue.issueKey()));
            boardReadModel.refreshIssue(issue);
            collaboration.autoWatchIssue(issue);
            return mapper.toResponse(issue);
        });
    }

    @Transactional(readOnly = true)
    public BoardResponse board(UUID projectId) {
        project(projectId);
        return boardReadModel.board(projectId);
    }

    @Transactional
    public IssueResponse updateIssue(String issueId, UpdateIssueRequest request, String idempotencyKey) {
        return withIdempotency("updateIssue:" + issueId, request, idempotencyKey, IssueResponse.class, () -> {
            IssueEntity issue = issue(issueId);
            validateVersion(issue, request.version());
            UUID previousAssigneeId = issue.assignee() == null ? null : issue.assignee().id();
            UserEntity assignee = request.assigneeId() == null ? null : user(request.assigneeId());
            SprintEntity sprint = request.sprintId() == null ? null : sprint(request.sprintId());

            issue.updateFields(
                    request.title(),
                    request.description(),
                    request.priority() == null ? null : normalizePriority(request.priority()),
                    assignee,
                    sprint,
                    request.storyPoints()
            );

            IssueEntity saved = issues.saveAndFlush(issue);
            boardReadModel.refreshIssue(saved);
            collaboration.autoWatchIssue(saved);
            recordEvent(project(saved.projectId()), saved, saved.reporter(), "IssueUpdated", Map.of("issueKey", saved.issueKey()));
            collaboration.notifyIssueParticipants(saved, saved.reporter(), "IssueUpdated", Map.of("issueKey", saved.issueKey()));
            if (assigneeChanged(previousAssigneeId, saved.assignee())) {
                collaboration.notifyIssueParticipants(
                        saved,
                        saved.reporter(),
                        "AssignmentChanged",
                        Map.of(
                                "issueKey", saved.issueKey(),
                                "assigneeId", saved.assignee().id()
                        )
                );
            }
            return mapper.toResponse(saved);
        });
    }

    @Transactional
    public IssueResponse transitionIssue(String issueId, TransitionIssueRequest request, String idempotencyKey) {
        return withIdempotency("transitionIssue:" + issueId, request, idempotencyKey, IssueResponse.class, () -> {
            IssueEntity issue = issue(issueId);
            validateVersion(issue, request.version());
            WorkflowStatusEntity targetStatus = statusByName(issue.projectId(), request.toStatus());
            WorkflowTransitionEntity transition = transitions
                    .findByProjectIdAndFromStatusIdAndToStatusId(issue.projectId(), issue.status().id(), targetStatus.id())
                    .orElseThrow(() -> workflowViolation(issue));
            validateTransitionRules(issue, transition);
            enforceWipLimit(issue, targetStatus);

            UserEntity reviewer = resolveTransitionAssignee(issue, transition);
            issue.transitionTo(targetStatus, reviewer);

            IssueEntity saved = issues.saveAndFlush(issue);
            boardReadModel.refreshIssue(saved);
            collaboration.autoWatchIssue(saved);
            recordEvent(
                    project(saved.projectId()),
                    saved,
                    saved.reporter(),
                    "StatusChanged",
                    Map.of("issueKey", saved.issueKey(), "toStatus", targetStatus.name())
            );
            collaboration.notifyIssueParticipants(
                    saved,
                    saved.reporter(),
                    "StatusChanged",
                    Map.of("issueKey", saved.issueKey(), "toStatus", targetStatus.name())
            );
            return mapper.toResponse(saved);
        });
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, String cursor, int limit) {
        if (query == null || query.isBlank()) {
            return new SearchResponse(List.of(), null);
        }
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        SearchCriteria criteria = parseSearch(query.trim());
        List<IssueEntity> results = issues.searchAdvanced(
                criteria.projectId(),
                criteria.textQuery(),
                criteria.status(),
                criteria.assignee(),
                criteria.priority(),
                criteria.type(),
                cursor,
                boundedLimit + 1
        );
        String nextCursor = results.size() > boundedLimit
                ? results.get(boundedLimit - 1).updatedAt().toString()
                : null;
        List<IssueResponse> items = results.stream()
                .limit(boundedLimit)
                .map(mapper::toResponse)
                .toList();
        return new SearchResponse(items, nextCursor);
    }

    private SearchCriteria parseSearch(String query) {
        Matcher matcher = STRUCTURED_FILTER.matcher(query);
        String status = null;
        String assignee = null;
        String priority = null;
        String type = null;
        UUID projectId = null;
        String textQuery = query.replaceAll("(?i)\\s+AND\\s+", " ");
        while (matcher.find()) {
            String key = matcher.group(1) == null ? matcher.group(3) : matcher.group(1);
            String value = matcher.group(2) == null ? matcher.group(4) : matcher.group(2);
            switch (key.toLowerCase()) {
                case "status" -> status = value;
                case "assignee" -> assignee = value;
                case "priority" -> priority = value;
                case "type" -> type = normalizeIssueType(value);
                case "projectid" -> projectId = UUID.fromString(value);
                default -> {
                }
            }
            textQuery = textQuery.replace(matcher.group(0), " ");
        }
        textQuery = textQuery.replaceAll("(?i)\\bAND\\b", " ").trim();
        if (textQuery.isBlank()) {
            textQuery = null;
        }
        return new SearchCriteria(projectId, textQuery, status, assignee, priority, type);
    }

    private <T> T withIdempotency(String operation, Object request, String key, Class<T> responseType, Supplier<T> supplier) {
        if (key == null || key.isBlank()) {
            return supplier.get();
        }

        String fingerprint = fingerprint(operation, request);
        return idempotencyKeys.findByIdempotencyKey(key)
                .map(existing -> replayOrReject(existing, fingerprint, responseType))
                .orElseGet(() -> {
                    T response = supplier.get();
                    idempotencyKeys.save(new IdempotencyKeyEntity(
                            UUID.randomUUID(),
                            key,
                            fingerprint,
                            200,
                            writeJson(response),
                            Instant.now(),
                            Instant.now().plus(24, ChronoUnit.HOURS)
                    ));
                    return response;
                });
    }

    private <T> T replayOrReject(IdempotencyKeyEntity existing, String fingerprint, Class<T> responseType) {
        if (!existing.requestFingerprint().equals(fingerprint)) {
            throw new DomainException(
                    ErrorCode.IDEMPOTENCY_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Idempotency-Key was already used with a different request"
            );
        }
        if (!existing.hasStoredResponse()) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Request is already in progress");
        }
        try {
            return objectMapper.readValue(existing.responseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to replay idempotent response");
        }
    }

    private void recordEvent(ProjectEntity project, IssueEntity issue, UserEntity actor, String eventType, Map<String, Object> payload) {
        String payloadJson = writeJson(payload);
        Instant now = Instant.now();
        activityLog.save(new ActivityLogEntity(UUID.randomUUID(), project, issue, actor, eventType, payloadJson, now));
        outbox.save(new DomainEventOutboxEntity(UUID.randomUUID(), "Issue", issue.id(), eventType, payloadJson, now));
        realTimeEvents.publishIssueEvent(project.id(), issue.issueKey(), eventType, payload);
    }

    private void validateVersion(IssueEntity issue, Long expectedVersion) {
        if (issue.version() != expectedVersion) {
            throw new DomainException(
                    ErrorCode.CONFLICT,
                    HttpStatus.CONFLICT,
                    "Issue version conflict. Current version is " + issue.version()
            );
        }
    }

    private boolean assigneeChanged(UUID previousAssigneeId, UserEntity assignee) {
        return assignee != null && !assignee.id().equals(previousAssigneeId);
    }

    private DomainException workflowViolation(IssueEntity issue) {
        List<String> allowed = transitions.findByProjectIdAndFromStatusId(issue.projectId(), issue.status().id())
                .stream()
                .map(transition -> transition.toStatus().name())
                .toList();
        return new DomainException(
                ErrorCode.WORKFLOW_VIOLATION,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Transition is not allowed from " + issue.status().name() + ". Allowed transitions: " + allowed
        );
    }

    private void enforceWipLimit(IssueEntity issue, WorkflowStatusEntity targetStatus) {
        WorkflowStatusEntity lockedTargetStatus = statuses.lockById(targetStatus.id()).orElse(targetStatus);
        Integer limit = lockedTargetStatus.wipLimit();
        if (limit == null) {
            return;
        }
        long currentWorkInProgress = issues.countByProjectIdAndStatusId(issue.projectId(), targetStatus.id());
        if (currentWorkInProgress >= limit) {
            throw new DomainException(
                    ErrorCode.CONFLICT,
                    HttpStatus.CONFLICT,
                    "WIP limit exceeded for " + targetStatus.name() + ". Limit is " + limit
            );
        }
    }

    private void validateTransitionRules(IssueEntity issue, WorkflowTransitionEntity transition) {
        String rules = transition.ruleConfig();
        if (rules == null || rules.isBlank() || "{}".equals(rules)) {
            return;
        }
        if (rules.contains("\"requireAssignee\": true") && issue.assignee() == null) {
            throw new DomainException(ErrorCode.WORKFLOW_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, "Transition requires an assignee");
        }
        if (rules.contains("\"requireStoryPoints\": true") && issue.storyPoints() == null) {
            throw new DomainException(ErrorCode.WORKFLOW_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, "Transition requires story points");
        }
    }

    private UserEntity resolveTransitionAssignee(IssueEntity issue, WorkflowTransitionEntity transition) {
        String actions = transition.actionConfig();
        if (actions == null || actions.isBlank()) {
            return null;
        }
        if (actions.contains("\"assignReviewer\": true") || actions.contains("\"assignProjectLead\": true")) {
            return firstProjectLead(issue.projectId());
        }
        return null;
    }

    private ProjectEntity project(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project not found"));
    }

    private UserEntity user(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
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

    private WorkflowStatusEntity firstStatus(UUID projectId) {
        return statuses.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project workflow has no statuses"));
    }

    private WorkflowStatusEntity statusByName(UUID projectId, String name) {
        return statuses.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Workflow status not found"));
    }

    private UserEntity firstProjectLead(UUID projectId) {
        return memberships.findByProjectIdAndRole(projectId, "project_lead")
                .stream()
                .findFirst()
                .map(membership -> membership.user())
                .orElse(null);
    }

    private String normalizeIssueType(String type) {
        return type.trim().toLowerCase().replace("-", "_");
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "medium";
        }
        return priority.trim().toLowerCase();
    }

    private String fingerprint(String operation, Object request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((operation + ":" + writeJson(request)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 is unavailable");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON");
        }
    }

    private record SearchCriteria(
            UUID projectId,
            String textQuery,
            String status,
            String assignee,
            String priority,
            String type
    ) {
    }
}
