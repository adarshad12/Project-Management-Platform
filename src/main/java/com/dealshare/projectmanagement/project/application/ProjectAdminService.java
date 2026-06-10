package com.dealshare.projectmanagement.project.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ActivityLogEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.DomainEventOutboxEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipId;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectMembershipJpaRepository;
import com.dealshare.projectmanagement.project.api.ProjectMembershipResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAdminService {

    private final ProjectJpaRepository projects;
    private final ProjectMembershipJpaRepository memberships;
    private final ActivityLogJpaRepository activityLog;
    private final DomainEventOutboxJpaRepository outbox;
    private final ObjectMapper objectMapper;

    public ProjectAdminService(
            ProjectJpaRepository projects,
            ProjectMembershipJpaRepository memberships,
            ActivityLogJpaRepository activityLog,
            DomainEventOutboxJpaRepository outbox,
            ObjectMapper objectMapper
    ) {
        this.projects = projects;
        this.memberships = memberships;
        this.activityLog = activityLog;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProjectMembershipResponse updateRole(UUID projectId, UUID userId, String role) {
        String normalizedRole = normalizeRole(role);
        ProjectMembershipEntity membership = memberships.findById(new ProjectMembershipId(projectId, userId))
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project membership not found"));
        String previousRole = membership.role();
        membership.changeRole(normalizedRole);
        recordSensitiveEvent(project(projectId), "RoleChanged", Map.of(
                "userId", userId,
                "previousRole", previousRole,
                "newRole", normalizedRole
        ));
        return new ProjectMembershipResponse(projectId, userId, normalizedRole);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        ProjectEntity project = project(projectId);
        recordSensitiveEvent(project, "ProjectDeleted", Map.of("projectId", projectId, "projectKey", project.key()));
        projects.delete(project);
    }

    private void recordSensitiveEvent(ProjectEntity project, String eventType, Map<String, Object> payload) {
        String payloadJson = writeJson(payload);
        Instant now = Instant.now();
        activityLog.save(new ActivityLogEntity(UUID.randomUUID(), project, null, null, eventType, payloadJson, now));
        outbox.save(new DomainEventOutboxEntity(UUID.randomUUID(), "Project", project.id(), eventType, payloadJson, now));
    }

    private ProjectEntity project(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project not found"));
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toLowerCase().replace("-", "_");
        if (!List.of("admin", "project_lead", "member", "viewer").contains(normalized)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Unsupported project role: " + role);
        }
        return normalized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON");
        }
    }
}
