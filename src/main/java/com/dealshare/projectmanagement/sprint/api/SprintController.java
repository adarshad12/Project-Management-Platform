package com.dealshare.projectmanagement.sprint.api;

import com.dealshare.projectmanagement.issue.api.IssueResponse;
import com.dealshare.projectmanagement.security.ProjectRole;
import com.dealshare.projectmanagement.security.RbacService;
import com.dealshare.projectmanagement.sprint.application.SprintService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SprintController {

    private final SprintService sprintService;
    private final RbacService rbac;

    public SprintController(SprintService sprintService, RbacService rbac) {
        this.sprintService = sprintService;
        this.rbac = rbac;
    }

    @GetMapping("/projects/{projectId}/sprints")
    List<SprintResponse> listSprints(@PathVariable UUID projectId) {
        rbac.requireProjectRole(projectId, ProjectRole.VIEWER);
        return sprintService.listSprints(projectId);
    }

    @PostMapping("/projects/{projectId}/sprints")
    SprintResponse createSprint(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateSprintRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireProjectRole(projectId, ProjectRole.PROJECT_LEAD);
        return sprintService.createSprint(projectId, request, idempotencyKey);
    }

    @PatchMapping("/sprints/{sprintId}")
    SprintResponse updateSprint(
            @PathVariable UUID sprintId,
            @RequestBody UpdateSprintRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireSprintRole(sprintId, ProjectRole.PROJECT_LEAD);
        return sprintService.updateSprint(sprintId, request, idempotencyKey);
    }

    @DeleteMapping("/sprints/{sprintId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSprint(@PathVariable UUID sprintId) {
        rbac.requireSprintRole(sprintId, ProjectRole.PROJECT_LEAD);
        sprintService.deleteSprint(sprintId);
    }

    @PostMapping("/sprints/{sprintId}/start")
    SprintResponse startSprint(
            @PathVariable UUID sprintId,
            @RequestBody StartSprintRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireSprintRole(sprintId, ProjectRole.PROJECT_LEAD);
        return sprintService.startSprint(sprintId, request, idempotencyKey);
    }

    @PostMapping("/sprints/{sprintId}/complete")
    SprintCompletionResponse completeSprint(
            @PathVariable UUID sprintId,
            @RequestBody CompleteSprintRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireSprintRole(sprintId, ProjectRole.PROJECT_LEAD);
        return sprintService.completeSprint(sprintId, request, idempotencyKey);
    }

    @PostMapping("/sprints/{sprintId}/issues")
    IssueResponse moveIssueToSprint(
            @PathVariable UUID sprintId,
            @Valid @RequestBody MoveIssueToSprintRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        rbac.requireSprintRole(sprintId, ProjectRole.MEMBER);
        return sprintService.moveIssueToSprint(sprintId, request.issueId(), idempotencyKey);
    }

    @DeleteMapping("/sprints/{sprintId}/issues/{issueId}")
    IssueResponse moveIssueToBacklog(@PathVariable UUID sprintId, @PathVariable String issueId) {
        rbac.requireSprintRole(sprintId, ProjectRole.MEMBER);
        return sprintService.moveIssueToBacklog(sprintId, issueId);
    }
}
