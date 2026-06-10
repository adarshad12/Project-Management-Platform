package com.dealshare.projectmanagement.realtime.api;

import com.dealshare.projectmanagement.realtime.application.RealTimeReplayService;
import com.dealshare.projectmanagement.security.ProjectRole;
import com.dealshare.projectmanagement.security.RbacService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/realtime")
public class RealTimeRestController {

    private final RealTimeReplayService replayService;
    private final RbacService rbac;

    public RealTimeRestController(RealTimeReplayService replayService, RbacService rbac) {
        this.replayService = replayService;
        this.rbac = rbac;
    }

    @GetMapping("/replay")
    ReplayResponse replay(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String issueId,
            @RequestParam(required = false) UUID lastSeenEventId
    ) {
        if (issueId != null && !issueId.isBlank()) {
            rbac.requireIssueRole(issueId, ProjectRole.VIEWER);
        } else if (projectId != null) {
            rbac.requireProjectRole(projectId, ProjectRole.VIEWER);
        }
        return replayService.replay(new ReplayRequest(projectId, issueId, lastSeenEventId));
    }
}
