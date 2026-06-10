package com.dealshare.projectmanagement.issue.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.collaboration.application.CollaborationService;
import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowStatusEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkspaceEntity;
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
import com.dealshare.projectmanagement.issue.api.UpdateIssueRequest;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IssueServiceConcurrencyTest {

    @Mock private ProjectJpaRepository projects;
    @Mock private UserJpaRepository users;
    @Mock private SprintJpaRepository sprints;
    @Mock private IssueJpaRepository issues;
    @Mock private WorkflowStatusJpaRepository statuses;
    @Mock private WorkflowTransitionJpaRepository transitions;
    @Mock private ProjectMembershipJpaRepository memberships;
    @Mock private ActivityLogJpaRepository activityLog;
    @Mock private DomainEventOutboxJpaRepository outbox;
    @Mock private IdempotencyKeyJpaRepository idempotencyKeys;
    @Mock private CollaborationService collaboration;
    @Mock private RealTimeEventPublisher realTimeEvents;
    @Mock private BoardReadModelService boardReadModel;

    @Test
    void staleIssueUpdateVersionReturnsConflictBeforeMutation() {
        IssueService service = new IssueService(
                projects,
                users,
                sprints,
                issues,
                statuses,
                transitions,
                memberships,
                activityLog,
                outbox,
                idempotencyKeys,
                collaboration,
                realTimeEvents,
                boardReadModel,
                new IssueMapper(),
                new ObjectMapper()
        );
        when(issues.findByIssueKey("PROJ-1")).thenReturn(Optional.of(issue()));

        assertThatThrownBy(() -> service.updateIssue(
                "PROJ-1",
                new UpdateIssueRequest(7L, "stale", null, null, null, null, null),
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("version conflict")
                .asInstanceOf(InstanceOfAssertFactories.type(DomainException.class))
                .satisfies(exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo(ErrorCode.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    private IssueEntity issue() {
        Instant now = Instant.now();
        WorkspaceEntity workspace = new WorkspaceEntity(UUID.randomUUID(), "Workspace", now, now);
        ProjectEntity project = new ProjectEntity(UUID.randomUUID(), workspace, "PROJ", "Project", null, now, now);
        UserEntity reporter = new UserEntity(UUID.randomUUID(), "viewer@example.com", "Bob Chen", now);
        WorkflowStatusEntity status = new WorkflowStatusEntity(UUID.randomUUID(), project, "To Do", "todo", 1, null);
        return new IssueEntity(
                UUID.randomUUID(),
                project,
                "PROJ-1",
                "task",
                "Issue",
                null,
                status,
                "medium",
                null,
                reporter,
                null,
                null,
                null,
                now,
                now
        );
    }
}
