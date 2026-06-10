package com.dealshare.projectmanagement.issue.api;

import com.dealshare.projectmanagement.issue.application.IssueService;
import com.dealshare.projectmanagement.security.ProjectRole;
import com.dealshare.projectmanagement.security.RbacService;
import com.dealshare.projectmanagement.security.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
public class IssueController {

    private final IssueService issueService;
    private final RbacService rbac;
    private final SecurityContext securityContext;

    public IssueController(IssueService issueService, RbacService rbac, SecurityContext securityContext) {
        this.issueService = issueService;
        this.rbac = rbac;
        this.securityContext = securityContext;
    }

    @PostMapping("/projects/{projectId}/issues")
    IssueResponse createIssue(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireProjectRole(projectId, ProjectRole.MEMBER);
        rbac.requireSelfOrProjectRole(request.reporterId(), projectId, ProjectRole.PROJECT_LEAD);
        return issueService.createIssue(projectId, request, idempotencyKey);
    }

    @GetMapping("/projects/{projectId}/board")
    BoardResponse board(@PathVariable UUID projectId) {
        rbac.requireProjectRole(projectId, ProjectRole.VIEWER);
        return issueService.board(projectId);
    }

    @PatchMapping("/issues/{issueId}")
    IssueResponse updateIssue(
            @PathVariable String issueId,
            @Valid @RequestBody UpdateIssueRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireIssueRole(issueId, ProjectRole.MEMBER);
        return issueService.updateIssue(issueId, request, idempotencyKey);
    }

    @PostMapping("/issues/{issueId}/transitions")
    IssueResponse transitionIssue(
            @PathVariable String issueId,
            @Valid @RequestBody TransitionIssueRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireIssueRole(issueId, ProjectRole.MEMBER);
        return issueService.transitionIssue(issueId, request, idempotencyKey);
    }

    @GetMapping("/search")
    SearchResponse search(
            @RequestParam String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit
    ) {
        securityContext.currentUser();
        return issueService.search(q, cursor, limit);
    }
}
