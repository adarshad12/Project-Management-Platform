package com.dealshare.projectmanagement.realtime.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.realtime.api.RealTimeEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealTimeEventPublisher {

    private static final int MAX_REPLAY_EVENTS = 100;
    private static final String PROJECT_STREAM_KEY = "realtime:project:%s:events";
    private static final String ISSUE_STREAM_KEY = "realtime:issue:%s:events";

    private final SimpMessagingTemplate messaging;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RealTimeEventPublisher(SimpMessagingTemplate messaging, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.messaging = messaging;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public RealTimeEventResponse publishIssueEvent(
            UUID projectId,
            String issueKey,
            String eventType,
            Map<String, Object> payload
    ) {
        RealTimeEventResponse event = event(projectId, issueKey, null, toClientEventType(eventType), payload);
        try {
            store(event, PROJECT_STREAM_KEY.formatted(projectId));
            store(event, ISSUE_STREAM_KEY.formatted(issueKey));
            messaging.convertAndSend("/topic/projects/" + projectId + "/board", event);
            messaging.convertAndSend("/topic/issues/" + issueKey, event);
        } catch (RuntimeException ignored) {
            // Real-time sync is best-effort; durable activity/outbox rows remain the source of truth.
        }
        return event;
    }

    public RealTimeEventResponse publishSprintEvent(
            UUID projectId,
            UUID sprintId,
            String eventType,
            Map<String, Object> payload
    ) {
        RealTimeEventResponse event = event(projectId, null, sprintId, toClientEventType(eventType), payload);
        try {
            store(event, PROJECT_STREAM_KEY.formatted(projectId));
            messaging.convertAndSend("/topic/projects/" + projectId + "/board", event);
        } catch (RuntimeException ignored) {
            // Real-time sync is best-effort; durable activity/outbox rows remain the source of truth.
        }
        return event;
    }

    private RealTimeEventResponse event(
            UUID projectId,
            String issueKey,
            UUID sprintId,
            String type,
            Map<String, Object> payload
    ) {
        return new RealTimeEventResponse(
                UUID.randomUUID(),
                type,
                projectId,
                issueKey,
                sprintId,
                writeJson(payload),
                Instant.now()
        );
    }

    private void store(RealTimeEventResponse event, String key) {
        redis.opsForList().rightPush(key, writeJson(event));
        redis.opsForList().trim(key, -MAX_REPLAY_EVENTS, -1);
    }

    private String toClientEventType(String eventType) {
        return switch (eventType) {
            case "IssueCreated" -> "issue_created";
            case "IssueUpdated" -> "issue_updated";
            case "StatusChanged" -> "issue_moved";
            case "CommentAdded" -> "comment_added";
            case "SprintCreated", "SprintUpdated", "SprintStarted", "SprintCompleted" -> "sprint_updated";
            default -> eventType.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        };
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON");
        }
    }
}
