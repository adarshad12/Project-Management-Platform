package com.dealshare.projectmanagement.collaboration.api;

import com.dealshare.projectmanagement.collaboration.application.CollaborationService;
import com.dealshare.projectmanagement.security.ProjectRole;
import com.dealshare.projectmanagement.security.RbacService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CollaborationController {

    private final CollaborationService collaboration;
    private final RbacService rbac;

    public CollaborationController(CollaborationService collaboration, RbacService rbac) {
        this.collaboration = collaboration;
        this.rbac = rbac;
    }

    @GetMapping("/issues/{issueId}/comments")
    List<CommentResponse> listComments(
            @PathVariable String issueId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) {
        rbac.requireIssueRole(issueId, ProjectRole.VIEWER);
        return collaboration.listComments(issueId, limit);
    }

    @PostMapping("/issues/{issueId}/comments")
    CommentResponse addComment(
            @PathVariable String issueId,
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID projectId = rbac.issueProjectId(issueId);
        rbac.requireProjectRole(projectId, ProjectRole.MEMBER);
        rbac.requireSelfOrProjectRole(request.authorId(), projectId, ProjectRole.PROJECT_LEAD);
        return collaboration.addComment(issueId, request, idempotencyKey);
    }

    @GetMapping("/issues/{issueId}/watchers")
    List<WatcherResponse> listWatchers(@PathVariable String issueId) {
        rbac.requireIssueRole(issueId, ProjectRole.VIEWER);
        return collaboration.listWatchers(issueId);
    }

    @PostMapping("/issues/{issueId}/watchers")
    WatcherResponse watchIssue(
            @PathVariable String issueId,
            @Valid @RequestBody WatchIssueRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID projectId = rbac.issueProjectId(issueId);
        rbac.requireProjectRole(projectId, ProjectRole.MEMBER);
        rbac.requireSelfOrProjectRole(request.userId(), projectId, ProjectRole.PROJECT_LEAD);
        return collaboration.watchIssue(issueId, request.userId(), idempotencyKey);
    }

    @DeleteMapping("/issues/{issueId}/watchers/{userId}")
    void unwatchIssue(@PathVariable String issueId, @PathVariable UUID userId) {
        UUID projectId = rbac.issueProjectId(issueId);
        rbac.requireSelfOrProjectRole(userId, projectId, ProjectRole.PROJECT_LEAD);
        collaboration.unwatchIssue(issueId, userId);
    }

    @GetMapping("/users/{userId}/notifications")
    List<NotificationResponse> listNotifications(
            @PathVariable UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) {
        rbac.requireSelf(userId);
        return collaboration.listNotifications(userId, status, limit);
    }

    @GetMapping("/projects/{projectId}/activity")
    ActivityFeedResponse activity(
            @PathVariable UUID projectId,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String issueId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit
    ) {
        rbac.requireProjectRole(projectId, ProjectRole.VIEWER);
        return collaboration.activity(projectId, cursor, actorId, issueId, eventType, from, to, limit);
    }
}
