package com.dealshare.projectmanagement.collaboration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.common.idempotency.IdempotencyService;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ActivityLogJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.CommentJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueWatcherJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.NotificationJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceMentionTest {

    @Mock private ProjectJpaRepository projects;
    @Mock private IssueJpaRepository issues;
    @Mock private UserJpaRepository users;
    @Mock private CommentJpaRepository comments;
    @Mock private IssueWatcherJpaRepository watchers;
    @Mock private NotificationJpaRepository notifications;
    @Mock private ActivityLogJpaRepository activityLog;
    @Mock private DomainEventOutboxJpaRepository outbox;
    @Mock private RealTimeEventPublisher realTimeEvents;
    @Mock private IdempotencyKeyJpaRepository idempotencyKeys;

    @Test
    void extractsEmailMentionsIntoUsers() {
        UserEntity lead = new UserEntity(UUID.randomUUID(), "lead@example.com", "Ravi Lead", Instant.now());
        when(users.findByEmail("lead@example.com")).thenReturn(Optional.of(lead));

        CollaborationService service = new CollaborationService(
                projects,
                issues,
                users,
                comments,
                watchers,
                notifications,
                activityLog,
                outbox,
                realTimeEvents,
                new ObjectMapper(),
                notification -> {
                },
                new IdempotencyService(idempotencyKeys, new ObjectMapper())
        );

        assertThat(service.mentionedUsers("Please review @lead@example.com"))
                .containsExactly(lead);
    }
}
