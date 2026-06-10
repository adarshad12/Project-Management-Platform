package com.dealshare.projectmanagement.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectMembershipId;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.WorkspaceEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectMembershipJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
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
class RbacServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");

    @Mock
    private SecurityContext securityContext;

    @Mock
    private ProjectMembershipJpaRepository memberships;

    @Mock
    private ProjectJpaRepository projects;

    @Mock
    private IssueJpaRepository issues;

    @Mock
    private SprintJpaRepository sprints;

    @Test
    void viewerCanReadButCannotMutate() {
        RbacService rbac = new RbacService(securityContext, memberships, projects, issues, sprints);
        ProjectEntity project = project();
        UserEntity user = new UserEntity(USER_ID, "viewer@example.com", "Bob Chen", Instant.now());
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(securityContext.currentUserId()).thenReturn(USER_ID);
        when(memberships.findById(new ProjectMembershipId(PROJECT_ID, USER_ID)))
                .thenReturn(Optional.of(new ProjectMembershipEntity(
                        new ProjectMembershipId(PROJECT_ID, USER_ID),
                        project,
                        user,
                        "viewer",
                        Instant.now()
                )));

        assertThatCode(() -> rbac.requireProjectRole(PROJECT_ID, ProjectRole.VIEWER)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rbac.requireProjectRole(PROJECT_ID, ProjectRole.MEMBER))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("member")
                .asInstanceOf(InstanceOfAssertFactories.type(DomainException.class))
                .satisfies(exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo(ErrorCode.FORBIDDEN);
                    org.assertj.core.api.Assertions.assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    private ProjectEntity project() {
        Instant now = Instant.now();
        WorkspaceEntity workspace = new WorkspaceEntity(UUID.randomUUID(), "Workspace", now, now);
        return new ProjectEntity(PROJECT_ID, workspace, "PROJ", "Project", null, now, now);
    }
}
