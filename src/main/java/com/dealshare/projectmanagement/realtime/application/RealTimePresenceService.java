package com.dealshare.projectmanagement.realtime.application;

import com.dealshare.projectmanagement.realtime.api.PresenceRequest;
import com.dealshare.projectmanagement.realtime.api.PresenceResponse;
import com.dealshare.projectmanagement.realtime.api.PresenceUserResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealTimePresenceService {

    private static final String SESSION_SCOPE_KEY = "realtime:presence:session:%s";
    private static final String BOARD_KEY = "realtime:presence:board:%s";
    private static final String ISSUE_KEY = "realtime:presence:issue:%s";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messaging;

    public RealTimePresenceService(StringRedisTemplate redis, ObjectMapper objectMapper, SimpMessagingTemplate messaging) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.messaging = messaging;
    }

    public PresenceResponse joinBoard(String sessionId, PresenceRequest request) {
        String key = BOARD_KEY.formatted(request.projectId());
        putPresence(key, sessionId, request);
        redis.opsForValue().set(SESSION_SCOPE_KEY.formatted(sessionId), key);
        PresenceResponse response = boardPresence(request, key);
        messaging.convertAndSend("/topic/projects/" + request.projectId() + "/presence", response);
        return response;
    }

    public PresenceResponse joinIssue(String sessionId, PresenceRequest request) {
        String key = ISSUE_KEY.formatted(request.issueId());
        putPresence(key, sessionId, request);
        redis.opsForValue().set(SESSION_SCOPE_KEY.formatted(sessionId), key);
        PresenceResponse response = issuePresence(request, key);
        messaging.convertAndSend("/topic/issues/" + request.issueId() + "/presence", response);
        return response;
    }

    public PresenceResponse leave(String sessionId, PresenceRequest request) {
        String key = redis.opsForValue().get(SESSION_SCOPE_KEY.formatted(sessionId));
        if (key != null) {
            redis.opsForHash().delete(key, sessionId);
            redis.delete(SESSION_SCOPE_KEY.formatted(sessionId));
        }
        if (request.issueId() != null && !request.issueId().isBlank()) {
            PresenceResponse response = issuePresence(request, ISSUE_KEY.formatted(request.issueId()));
            messaging.convertAndSend("/topic/issues/" + request.issueId() + "/presence", response);
            return response;
        }
        PresenceResponse response = boardPresence(request, BOARD_KEY.formatted(request.projectId()));
        messaging.convertAndSend("/topic/projects/" + request.projectId() + "/presence", response);
        return response;
    }

    public void disconnect(String sessionId) {
        String key = redis.opsForValue().get(SESSION_SCOPE_KEY.formatted(sessionId));
        if (key != null) {
            redis.opsForHash().delete(key, sessionId);
            redis.delete(SESSION_SCOPE_KEY.formatted(sessionId));
        }
    }

    private void putPresence(String key, String sessionId, PresenceRequest request) {
        PresenceUserResponse user = new PresenceUserResponse(
                request.userId(),
                request.displayName(),
                sessionId,
                Instant.now()
        );
        redis.opsForHash().put(key, sessionId, writeJson(user));
    }

    private PresenceResponse boardPresence(PresenceRequest request, String key) {
        return new PresenceResponse(request.projectId(), null, users(key));
    }

    private PresenceResponse issuePresence(PresenceRequest request, String key) {
        return new PresenceResponse(request.projectId(), request.issueId(), users(key));
    }

    private List<PresenceUserResponse> users(String key) {
        return redis.opsForHash().values(key)
                .stream()
                .map(String::valueOf)
                .map(this::readUser)
                .toList();
    }

    private PresenceUserResponse readUser(String json) {
        try {
            return objectMapper.readValue(json, PresenceUserResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read presence user", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write presence user", exception);
        }
    }
}
