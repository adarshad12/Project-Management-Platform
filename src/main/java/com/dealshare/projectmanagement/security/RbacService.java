package com.dealshare.projectmanagement.security;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipId;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectMembershipJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RbacService {

    private final SecurityContext securityContext;
    private final ProjectMembershipJpaRepository memberships;
    private final ProjectJpaRepository projects;
    private final IssueJpaRepository issues;
    private final SprintJpaRepository sprints;

    public RbacService(
            SecurityContext securityContext,
            ProjectMembershipJpaRepository memberships,
            ProjectJpaRepository projects,
            IssueJpaRepository issues,
            SprintJpaRepository sprints
    ) {
        this.securityContext = securityContext;
        this.memberships = memberships;
        this.projects = projects;
        this.issues = issues;
        this.sprints = sprints;
    }

    public void requireProjectRole(UUID projectId, ProjectRole required) {
        projects.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project not found"));
        ProjectRole actual = currentRole(projectId);
        if (!actual.atLeast(required)) {
            throw new DomainException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Project role " + required.value() + " or higher is required");
        }
    }

    public void requireIssueRole(String issueId, ProjectRole required) {
        requireProjectRole(issue(issueId).projectId(), required);
    }

    public void requireSprintRole(UUID sprintId, ProjectRole required) {
        SprintEntity sprint = sprints.findById(sprintId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Sprint not found"));
        requireProjectRole(sprint.projectId(), required);
    }

    public void requireSelfOrProjectRole(UUID userId, UUID projectId, ProjectRole required) {
        if (securityContext.currentUserId().equals(userId)) {
            return;
        }
        requireProjectRole(projectId, required);
    }

    public void requireSelf(UUID userId) {
        if (!securityContext.currentUserId().equals(userId)) {
            throw new DomainException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "User can only access their own resource");
        }
    }

    public UUID issueProjectId(String issueId) {
        return issue(issueId).projectId();
    }

    private ProjectRole currentRole(UUID projectId) {
        UUID userId = securityContext.currentUserId();
        ProjectMembershipEntity membership = memberships.findById(new ProjectMembershipId(projectId, userId))
                .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Project membership is required"));
        return ProjectRole.fromValue(membership.role());
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
}
