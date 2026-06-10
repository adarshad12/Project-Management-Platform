package com.dealshare.projectmanagement.events.application;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.DomainEventOutboxEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.DomainEventOutboxJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.SprintJpaRepository;
import com.dealshare.projectmanagement.realtime.application.RealTimeEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(value = "workers.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private final DomainEventOutboxJpaRepository outbox;
    private final IssueJpaRepository issues;
    private final SprintJpaRepository sprints;
    private final RealTimeEventPublisher realTimeEvents;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(
            DomainEventOutboxJpaRepository outbox,
            IssueJpaRepository issues,
            SprintJpaRepository sprints,
            RealTimeEventPublisher realTimeEvents,
            ObjectMapper objectMapper
    ) {
        this.outbox = outbox;
        this.issues = issues;
        this.sprints = sprints;
        this.realTimeEvents = realTimeEvents;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.dispatch.fixed-delay-ms:5000}")
    @Transactional
    public void dispatchUnprocessedEvents() {
        outbox.findByProcessedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 100))
                .forEach(this::dispatch);
    }

    private void dispatch(DomainEventOutboxEntity event) {
        Map<String, Object> payload = readPayload(event.payload());
        if ("Issue".equals(event.aggregateType())) {
            issues.findById(event.aggregateId()).ifPresent(issue -> publishIssue(event, issue, payload));
        } else if ("Sprint".equals(event.aggregateType())) {
            sprints.findById(event.aggregateId()).ifPresent(sprint -> publishSprint(event, sprint, payload));
        } else {
            event.markProcessed(Instant.now());
        }
    }

    private void publishIssue(DomainEventOutboxEntity event, IssueEntity issue, Map<String, Object> payload) {
        realTimeEvents.publishIssueEvent(issue.projectId(), issue.issueKey(), event.eventType(), payload);
        event.markProcessed(Instant.now());
    }

    private void publishSprint(DomainEventOutboxEntity event, SprintEntity sprint, Map<String, Object> payload) {
        realTimeEvents.publishSprintEvent(sprint.projectId(), sprint.id(), event.eventType(), payload);
        event.markProcessed(Instant.now());
    }

    private Map<String, Object> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
