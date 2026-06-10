package com.dealshare.projectmanagement.project.api;

import com.dealshare.projectmanagement.project.application.ProjectAdminService;
import com.dealshare.projectmanagement.security.ProjectRole;
import com.dealshare.projectmanagement.security.RbacService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectAdminController {

    private final ProjectAdminService projects;
    private final RbacService rbac;

    public ProjectAdminController(ProjectAdminService projects, RbacService rbac) {
        this.projects = projects;
        this.rbac = rbac;
    }

    @PatchMapping("/{projectId}/members/{userId}/role")
    ProjectMembershipResponse updateRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProjectRoleRequest request
    ) {
        rbac.requireProjectRole(projectId, ProjectRole.ADMIN);
        return projects.updateRole(projectId, userId, request.role());
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProject(@PathVariable UUID projectId) {
        rbac.requireProjectRole(projectId, ProjectRole.ADMIN);
        projects.deleteProject(projectId);
    }
}
