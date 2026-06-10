package com.dealshare.projectmanagement.issue.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.collaboration.application.CollaborationService;
import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowStatusEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowTransitionEntity;
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
import com.dealshare.projectmanagement.issue.api.TransitionIssueRequest;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IssueServiceWorkflowTest {

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
    void invalidWorkflowTransitionReturnsUnprocessableEntityWithAllowedTransitions() {
        Fixture fixture = fixture();
        WorkflowStatusEntity done = new WorkflowStatusEntity(UUID.randomUUID(), fixture.project, "Done", "done", 4, null);
        WorkflowStatusEntity inProgress = new WorkflowStatusEntity(UUID.randomUUID(), fixture.project, "In Progress", "in_progress", 2, null);
        WorkflowTransitionEntity allowedTransition = org.mockito.Mockito.mock(WorkflowTransitionEntity.class);

        when(issues.findByIssueKey("PROJ-1")).thenReturn(Optional.of(fixture.issue));
        when(statuses.findByProjectIdAndName(fixture.project.id(), "Done")).thenReturn(Optional.of(done));
        when(transitions.findByProjectIdAndFromStatusIdAndToStatusId(
                fixture.project.id(),
                fixture.todo.id(),
                done.id()
        )).thenReturn(Optional.empty());
        when(transitions.findByProjectIdAndFromStatusId(fixture.project.id(), fixture.todo.id()))
                .thenReturn(List.of(allowedTransition));
        when(allowedTransition.toStatus()).thenReturn(inProgress);

        assertThatThrownBy(() -> service().transitionIssue("PROJ-1", new TransitionIssueRequest(0L, "Done"), null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Transition is not allowed from To Do")
                .hasMessageContaining("In Progress")
                .asInstanceOf(InstanceOfAssertFactories.type(DomainException.class))
                .satisfies(exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.WORKFLOW_VIOLATION);
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(issues, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(IssueEntity.class));
    }

    private IssueService service() {
        return new IssueService(
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
    }

    private Fixture fixture() {
        Instant now = Instant.now();
        WorkspaceEntity workspace = new WorkspaceEntity(UUID.randomUUID(), "Workspace", now, now);
        ProjectEntity project = new ProjectEntity(UUID.randomUUID(), workspace, "PROJ", "Project", null, now, now);
        UserEntity reporter = new UserEntity(UUID.randomUUID(), "reporter@example.com", "Reporter", now);
        WorkflowStatusEntity todo = new WorkflowStatusEntity(UUID.randomUUID(), project, "To Do", "todo", 1, null);
        IssueEntity issue = new IssueEntity(
                UUID.randomUUID(),
                project,
                "PROJ-1",
                "task",
                "Issue",
                null,
                todo,
                "medium",
                null,
                reporter,
                null,
                null,
                null,
                now,
                now
        );
        return new Fixture(project, todo, issue);
    }

    private record Fixture(ProjectEntity project, WorkflowStatusEntity todo, IssueEntity issue) {
    }
}
