package com.dealshare.projectmanagement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.BoardIssueReadModelJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.CommentJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.CustomFieldDefinitionJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueCustomFieldValueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueWatcherJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.NotificationJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectMembershipJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkflowStatusJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkflowTransitionJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkspaceJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceRepositorySmokeTest {

    @Autowired
    private WorkspaceJpaRepository workspaces;

    @Autowired
    private UserJpaRepository users;

    @Autowired
    private ProjectJpaRepository projects;

    @Autowired
    private ProjectMembershipJpaRepository memberships;

    @Autowired
    private WorkflowStatusJpaRepository workflowStatuses;

    @Autowired
    private WorkflowTransitionJpaRepository workflowTransitions;

    @Autowired
    private SprintJpaRepository sprints;

    @Autowired
    private IssueJpaRepository issues;

    @Autowired
    private BoardIssueReadModelJpaRepository boardIssueReadModel;

    @Autowired
    private IssueWatcherJpaRepository watchers;

    @Autowired
    private CommentJpaRepository comments;

    @Autowired
    private CustomFieldDefinitionJpaRepository customFieldDefinitions;

    @Autowired
    private IssueCustomFieldValueJpaRepository customFieldValues;

    @Autowired
    private ActivityLogJpaRepository activityLog;

    @Autowired
    private DomainEventOutboxJpaRepository outbox;

    @Autowired
    private NotificationJpaRepository notifications;

    @Autowired
    private IdempotencyKeyJpaRepository idempotencyKeys;

    @Test
    void repositoriesStartWithApplicationContext() {
        assertThat(workspaces).isNotNull();
        assertThat(users).isNotNull();
        assertThat(projects).isNotNull();
        assertThat(memberships).isNotNull();
        assertThat(workflowStatuses).isNotNull();
        assertThat(workflowTransitions).isNotNull();
        assertThat(sprints).isNotNull();
        assertThat(issues).isNotNull();
        assertThat(boardIssueReadModel).isNotNull();
        assertThat(watchers).isNotNull();
        assertThat(comments).isNotNull();
        assertThat(customFieldDefinitions).isNotNull();
        assertThat(customFieldValues).isNotNull();
        assertThat(activityLog).isNotNull();
        assertThat(outbox).isNotNull();
        assertThat(notifications).isNotNull();
        assertThat(idempotencyKeys).isNotNull();
    }
}
