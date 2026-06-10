package com.dealshare.projectmanagement.sprint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.common.idempotency.IdempotencyService;
import com.dealshare.projectmanagement.infrastructure.persistence.AdvisoryLockRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintCompletionIssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkflowStatusEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkspaceEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintCompletionIssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.issue.application.BoardReadModelService;
import com.dealshare.projectmanagement.issue.application.IssueMapper;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.dealshare.projectmanagement.security.SecurityContext;
import com.dealshare.projectmanagement.sprint.api.CompleteSprintRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SprintServiceCarryOverTest {

    @Mock private ProjectJpaRepository projects;
    @Mock private SprintJpaRepository sprints;
    @Mock private IssueJpaRepository issues;
    @Mock private UserJpaRepository users;
    @Mock private SprintCompletionIssueJpaRepository sprintCompletionIssues;
    @Mock private AdvisoryLockRepository advisoryLocks;
    @Mock private ActivityLogJpaRepository activityLog;
    @Mock private DomainEventOutboxJpaRepository outbox;
    @Mock private IdempotencyKeyJpaRepository idempotencyKeys;
    @Mock private RealTimeEventPublisher realTimeEvents;
    @Mock private SecurityContext securityContext;
    @Mock private BoardReadModelService boardReadModel;

    @Test
    void completeSprintCarriesSelectedIncompleteIssuesAndMovesOthersToBacklog() {
        Fixture fixture = fixture();
        SprintEntity targetSprint = sprint(fixture.project, "Sprint 2", "planned");
        IssueEntity completed = issue(fixture.project, fixture.activeSprint, fixture.done, "PROJ-1", 5);
        IssueEntity carried = issue(fixture.project, fixture.activeSprint, fixture.todo, "PROJ-2", 3);
        IssueEntity backlog = issue(fixture.project, fixture.activeSprint, fixture.todo, "PROJ-3", 5);
        UserEntity actor = new UserEntity(UUID.randomUUID(), "lead@example.com", "Lead", Instant.now());

        when(sprints.findById(fixture.activeSprint.id())).thenReturn(Optional.of(fixture.activeSprint));
        when(sprints.findById(targetSprint.id())).thenReturn(Optional.of(targetSprint));
        when(issues.findBySprintId(fixture.activeSprint.id())).thenReturn(List.of(completed, carried, backlog));
        when(sprints.save(fixture.activeSprint)).thenReturn(fixture.activeSprint);
        when(projects.findById(fixture.project.id())).thenReturn(Optional.of(fixture.project));
        when(securityContext.currentUserId()).thenReturn(actor.id());
        when(users.findById(actor.id())).thenReturn(Optional.of(actor));

        var response = service().completeSprint(
                fixture.activeSprint.id(),
                new CompleteSprintRequest(targetSprint.id(), List.of(carried.issueKey())),
                null
        );

        assertThat(response.sprint().status()).isEqualTo("completed");
        assertThat(response.sprint().completedStoryPoints()).isEqualTo(5);
        assertThat(response.sprint().carriedOverStoryPoints()).isEqualTo(8);
        assertThat(response.completedIssues()).extracting("issueId").containsExactly("PROJ-1");
        assertThat(response.carriedOverIssues()).extracting("issueId").containsExactly("PROJ-2");
        assertThat(response.movedToBacklogIssues()).extracting("issueId").containsExactly("PROJ-3");
        assertThat(carried.sprint()).isEqualTo(targetSprint);
        assertThat(backlog.sprint()).isNull();

        ArgumentCaptor<SprintCompletionIssueEntity> completionCaptor =
                ArgumentCaptor.forClass(SprintCompletionIssueEntity.class);
        verify(sprintCompletionIssues, org.mockito.Mockito.times(3)).save(completionCaptor.capture());
        assertThat(completionCaptor.getAllValues())
                .extracting(row -> ReflectionTestUtils.getField(row, "outcome"))
                .containsExactlyInAnyOrder("completed", "carried_over", "moved_to_backlog");
        verify(advisoryLocks).lockSprint(fixture.activeSprint.id());
        verify(boardReadModel).refreshIssues(List.of(completed, carried, backlog));
    }

    @Test
    void completeSprintRejectsCarryOverWithoutTargetSprint() {
        Fixture fixture = fixture();
        IssueEntity carried = issue(fixture.project, fixture.activeSprint, fixture.todo, "PROJ-2", 3);
        when(sprints.findById(fixture.activeSprint.id())).thenReturn(Optional.of(fixture.activeSprint));
        when(issues.findBySprintId(fixture.activeSprint.id())).thenReturn(List.of(carried));

        assertThatThrownBy(() -> service().completeSprint(
                fixture.activeSprint.id(),
                new CompleteSprintRequest(null, List.of(carried.issueKey())),
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("targetSprintId is required")
                .asInstanceOf(InstanceOfAssertFactories.type(DomainException.class))
                .satisfies(exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(advisoryLocks).lockSprint(fixture.activeSprint.id());
    }

    private SprintService service() {
        return new SprintService(
                projects,
                sprints,
                issues,
                users,
                sprintCompletionIssues,
                advisoryLocks,
                activityLog,
                outbox,
                realTimeEvents,
                new SprintMapper(),
                new IssueMapper(),
                new ObjectMapper(),
                securityContext,
                boardReadModel,
                new IdempotencyService(idempotencyKeys, new ObjectMapper())
        );
    }

    private Fixture fixture() {
        Instant now = Instant.now();
        WorkspaceEntity workspace = new WorkspaceEntity(UUID.randomUUID(), "Workspace", now, now);
        ProjectEntity project = new ProjectEntity(UUID.randomUUID(), workspace, "PROJ", "Project", null, now, now);
        SprintEntity activeSprint = sprint(project, "Sprint 1", "active");
        WorkflowStatusEntity todo = new WorkflowStatusEntity(UUID.randomUUID(), project, "To Do", "todo", 1, null);
        WorkflowStatusEntity done = new WorkflowStatusEntity(UUID.randomUUID(), project, "Done", "done", 4, null);
        return new Fixture(project, activeSprint, todo, done);
    }

    private SprintEntity sprint(ProjectEntity project, String name, String status) {
        return new SprintEntity(
                UUID.randomUUID(),
                project,
                name,
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                status,
                Instant.now(),
                Instant.now()
        );
    }

    private IssueEntity issue(ProjectEntity project, SprintEntity sprint, WorkflowStatusEntity status, String issueKey, int points) {
        Instant now = Instant.now();
        UserEntity reporter = new UserEntity(UUID.randomUUID(), "reporter-" + issueKey + "@example.com", "Reporter", now);
        return new IssueEntity(
                UUID.randomUUID(),
                project,
                issueKey,
                "story",
                issueKey,
                null,
                status,
                "medium",
                null,
                reporter,
                sprint,
                points,
                null,
                now,
                now
        );
    }

    private record Fixture(
            ProjectEntity project,
            SprintEntity activeSprint,
            WorkflowStatusEntity todo,
            WorkflowStatusEntity done
    ) {
    }
}
