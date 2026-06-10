package com.dealshare.projectmanagement.collaboration.application;

import com.dealshare.projectmanagement.collaboration.api.ActivityFeedResponse;
import com.dealshare.projectmanagement.collaboration.api.ActivityResponse;
import com.dealshare.projectmanagement.collaboration.api.CommentResponse;
import com.dealshare.projectmanagement.collaboration.api.CreateCommentRequest;
import com.dealshare.projectmanagement.collaboration.api.NotificationResponse;
import com.dealshare.projectmanagement.collaboration.api.WatcherResponse;
import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.common.idempotency.IdempotencyService;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ActivityLogEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.CommentEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.DomainEventOutboxEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueWatcherEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueWatcherId;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.NotificationEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.CommentJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueWatcherJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.NotificationJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.issue.api.UserSummaryResponse;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationService {

    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9._%+\\-]+(?:@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})?)");
    private static final int NOTIFICATION_DELIVERY_FAILURE_THRESHOLD = 5;
    private static final Duration NOTIFICATION_DELIVERY_BREAK_DURATION = Duration.ofSeconds(30);

    private final ProjectJpaRepository projects;
    private final IssueJpaRepository issues;
    private final UserJpaRepository users;
    private final CommentJpaRepository comments;
    private final IssueWatcherJpaRepository watchers;
    private final NotificationJpaRepository notifications;
    private final ActivityLogJpaRepository activityLog;
    private final DomainEventOutboxJpaRepository outbox;
    private final RealTimeEventPublisher realTimeEvents;
    private final ObjectMapper objectMapper;
    private final NotificationDeliveryPort notificationDelivery;
    private final IdempotencyService idempotency;
    private final AtomicInteger notificationDeliveryFailures = new AtomicInteger();
    private final AtomicReference<Instant> notificationDeliveryOpenUntil = new AtomicReference<>(Instant.EPOCH);

    public CollaborationService(
            ProjectJpaRepository projects,
            IssueJpaRepository issues,
            UserJpaRepository users,
            CommentJpaRepository comments,
            IssueWatcherJpaRepository watchers,
            NotificationJpaRepository notifications,
            ActivityLogJpaRepository activityLog,
            DomainEventOutboxJpaRepository outbox,
            RealTimeEventPublisher realTimeEvents,
            ObjectMapper objectMapper,
            NotificationDeliveryPort notificationDelivery,
            IdempotencyService idempotency
    ) {
        this.projects = projects;
        this.issues = issues;
        this.users = users;
        this.comments = comments;
        this.watchers = watchers;
        this.notifications = notifications;
        this.activityLog = activityLog;
        this.outbox = outbox;
        this.realTimeEvents = realTimeEvents;
        this.objectMapper = objectMapper;
        this.notificationDelivery = notificationDelivery;
        this.idempotency = idempotency;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(String issueId, int limit) {
        IssueEntity issue = issue(issueId);
        List<CommentEntity> all = comments.findByIssueIdOrderByCreatedAtAsc(issue.id());
        Map<UUID, List<CommentEntity>> repliesByParent = all.stream()
                .filter(comment -> comment.parentComment() != null)
                .collect(Collectors.groupingBy(comment -> comment.parentComment().id(), LinkedHashMap::new, Collectors.toList()));

        return all.stream()
                .filter(comment -> comment.parentComment() == null)
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(comment -> toCommentResponse(comment, repliesByParent))
                .toList();
    }

    @Transactional
    public CommentResponse addComment(String issueId, CreateCommentRequest request, String idempotencyKey) {
        return idempotency.execute("addComment:" + issueId, request, idempotencyKey, CommentResponse.class, () -> addComment(issueId, request));
    }

    private CommentResponse addComment(String issueId, CreateCommentRequest request) {
        IssueEntity issue = issue(issueId);
        UserEntity author = user(request.authorId());
        CommentEntity parent = request.parentCommentId() == null ? null : comment(request.parentCommentId());
        if (parent != null && !parent.issue().id().equals(issue.id())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Parent comment must belong to the same issue");
        }

        Instant now = Instant.now();
        CommentEntity saved = comments.save(new CommentEntity(
                UUID.randomUUID(),
                issue,
                parent,
                author,
                request.body(),
                now,
                now
        ));

        Set<UserEntity> mentionedUsers = mentionedUsers(request.body());
        ensureWatcher(issue, author);
        mentionedUsers.forEach(user -> ensureWatcher(issue, user));

        Map<String, Object> payload = Map.of(
                "issueKey", issue.issueKey(),
                "commentId", saved.id(),
                "mentionedUserIds", mentionedUsers.stream().map(UserEntity::id).map(UUID::toString).toList()
        );
        recordIssueEvent(issue, author, "CommentAdded", payload);
        notifyIssueParticipants(issue, author, "CommentAdded", payload, mentionedUsers);

        return toCommentResponse(saved, Map.of());
    }

    @Transactional(readOnly = true)
    public List<WatcherResponse> listWatchers(String issueId) {
        IssueEntity issue = issue(issueId);
        return watchers.findByIssueId(issue.id())
                .stream()
                .map(watcher -> new WatcherResponse(toUserSummary(watcher.user()), watcher.createdAt()))
                .toList();
    }

    @Transactional
    public WatcherResponse watchIssue(String issueId, UUID userId, String idempotencyKey) {
        return idempotency.execute("watchIssue:" + issueId, userId, idempotencyKey, WatcherResponse.class, () -> watchIssue(issueId, userId));
    }

    private WatcherResponse watchIssue(String issueId, UUID userId) {
        IssueEntity issue = issue(issueId);
        UserEntity user = user(userId);
        IssueWatcherEntity watcher = ensureWatcher(issue, user);
        return new WatcherResponse(toUserSummary(watcher.user()), watcher.createdAt());
    }

    @Transactional
    public void unwatchIssue(String issueId, UUID userId) {
        IssueEntity issue = issue(issueId);
        user(userId);
        watchers.deleteByIssueIdAndUserId(issue.id(), userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications(UUID userId, String status, int limit) {
        user(userId);
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        List<NotificationEntity> rows = status == null || status.isBlank()
                ? notifications.findByUserIdOrderByCreatedAtDesc(userId, page)
                : notifications.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status.trim().toLowerCase(Locale.ROOT), page);
        return rows.stream().map(this::toNotificationResponse).toList();
    }

    @Transactional
    public void retryDueNotifications() {
        notifications.findDueForDelivery(Instant.now(), PageRequest.of(0, 100))
                .forEach(this::attemptNotificationDelivery);
    }

    @Transactional(readOnly = true)
    public ActivityFeedResponse activity(
            UUID projectId,
            Instant cursor,
            UUID actorId,
            String issueId,
            String eventType,
            Instant from,
            Instant to,
            int limit
    ) {
        project(projectId);
        UUID resolvedIssueId = issueId == null || issueId.isBlank() ? null : issue(issueId).id();
        Instant resolvedCursor = cursor == null ? Instant.now().plusSeconds(1) : cursor;
        Instant resolvedFrom = from == null ? Instant.EPOCH : from;
        Instant resolvedTo = to == null ? Instant.now().plusSeconds(1) : to;
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        List<ActivityLogEntity> rows = activityLog.findProjectActivity(
                projectId,
                resolvedCursor,
                actorId,
                resolvedIssueId,
                eventType == null || eventType.isBlank() ? null : eventType,
                resolvedFrom,
                resolvedTo,
                PageRequest.of(0, boundedLimit)
        );
        List<ActivityResponse> items = rows.stream().map(this::toActivityResponse).toList();
        String nextCursor = rows.size() == boundedLimit ? rows.get(rows.size() - 1).createdAt().toString() : null;
        return new ActivityFeedResponse(items, nextCursor);
    }

    @Transactional
    public void autoWatchIssue(IssueEntity issue) {
        ensureWatcher(issue, issue.reporter());
        if (issue.assignee() != null) {
            ensureWatcher(issue, issue.assignee());
        }
    }

    @Transactional
    public void notifyIssueParticipants(IssueEntity issue, UserEntity actor, String eventType, Map<String, Object> payload) {
        notifyIssueParticipants(issue, actor, eventType, payload, Set.of());
    }

    @Transactional
    public void notifyIssueParticipants(
            IssueEntity issue,
            UserEntity actor,
            String eventType,
            Map<String, Object> payload,
            Set<UserEntity> explicitRecipients
    ) {
        Set<UserEntity> recipients = new LinkedHashSet<>(explicitRecipients);
        watchers.findByIssueId(issue.id()).stream().map(IssueWatcherEntity::user).forEach(recipients::add);
        if (issue.assignee() != null) {
            recipients.add(issue.assignee());
        }
        recipients.removeIf(user -> actor != null && user.id().equals(actor.id()));

        recipients.forEach(recipient -> queueNotification(recipient, eventType, payload));
    }

    public Set<UserEntity> mentionedUsers(String body) {
        if (body == null || body.isBlank()) {
            return Set.of();
        }
        Set<UserEntity> mentioned = new LinkedHashSet<>();
        Matcher matcher = MENTION.matcher(body);
        while (matcher.find()) {
            String token = matcher.group(1);
            users.findByEmail(token)
                    .or(() -> users.findAll().stream()
                            .filter(user -> mentionKey(user.displayName()).equalsIgnoreCase(token))
                            .findFirst())
                    .ifPresent(mentioned::add);
        }
        return mentioned;
    }

    private IssueWatcherEntity ensureWatcher(IssueEntity issue, UserEntity user) {
        IssueWatcherId id = new IssueWatcherId(issue.id(), user.id());
        return watchers.findById(id)
                .orElseGet(() -> watchers.save(new IssueWatcherEntity(id, issue, user, Instant.now())));
    }

    private void queueNotification(UserEntity user, String eventType, Map<String, Object> payload) {
        NotificationEntity notification = notifications.save(new NotificationEntity(
                UUID.randomUUID(),
                user,
                eventType,
                writeJson(payload),
                "pending",
                Instant.now(),
                null
        ));
        attemptNotificationDelivery(notification);
    }

    private void attemptNotificationDelivery(NotificationEntity notification) {
        Instant now = Instant.now();
        if (notificationDeliveryOpenUntil.get().isAfter(now)) {
            return;
        }
        try {
            notificationDelivery.deliver(notification);
            notification.markDelivered(now);
            notificationDeliveryFailures.set(0);
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage(), now.plusSeconds(30));
            if (notificationDeliveryFailures.incrementAndGet() >= NOTIFICATION_DELIVERY_FAILURE_THRESHOLD) {
                notificationDeliveryOpenUntil.set(now.plus(NOTIFICATION_DELIVERY_BREAK_DURATION));
            }
        }
    }

    private void recordIssueEvent(IssueEntity issue, UserEntity actor, String eventType, Map<String, Object> payload) {
        String payloadJson = writeJson(payload);
        Instant now = Instant.now();
        ProjectEntity project = project(issue.projectId());
        activityLog.save(new ActivityLogEntity(UUID.randomUUID(), project, issue, actor, eventType, payloadJson, now));
        outbox.save(new DomainEventOutboxEntity(UUID.randomUUID(), "Issue", issue.id(), eventType, payloadJson, now));
        realTimeEvents.publishIssueEvent(project.id(), issue.issueKey(), eventType, payload);
    }

    private CommentResponse toCommentResponse(CommentEntity comment, Map<UUID, List<CommentEntity>> repliesByParent) {
        List<CommentResponse> replies = repliesByParent.getOrDefault(comment.id(), List.of())
                .stream()
                .map(reply -> toCommentResponse(reply, repliesByParent))
                .toList();
        return new CommentResponse(
                comment.id(),
                comment.issue().issueKey(),
                comment.parentComment() == null ? null : comment.parentComment().id(),
                toUserSummary(comment.author()),
                comment.body(),
                mentionedUsers(comment.body()).stream().map(this::toUserSummary).toList(),
                replies,
                comment.createdAt(),
                comment.updatedAt()
        );
    }

    private NotificationResponse toNotificationResponse(NotificationEntity notification) {
        return new NotificationResponse(
                notification.id(),
                notification.user().id(),
                notification.eventType(),
                notification.payload(),
                notification.status(),
                notification.createdAt(),
                notification.deliveredAt()
        );
    }

    private ActivityResponse toActivityResponse(ActivityLogEntity activity) {
        return new ActivityResponse(
                activity.id(),
                activity.project().id(),
                activity.issue() == null ? null : activity.issue().issueKey(),
                activity.actor() == null ? null : toUserSummary(activity.actor()),
                activity.eventType(),
                activity.payload(),
                activity.createdAt()
        );
    }

    private UserSummaryResponse toUserSummary(UserEntity user) {
        return new UserSummaryResponse(user.id(), user.displayName());
    }

    private ProjectEntity project(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project not found"));
    }

    private UserEntity user(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
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

    private CommentEntity comment(UUID commentId) {
        return comments.findById(commentId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private String mentionKey(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON");
        }
    }
}
