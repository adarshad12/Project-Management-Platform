package com.dealshare.projectmanagement.realtime.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.realtime.api.RealTimeEventResponse;
import com.dealshare.projectmanagement.realtime.api.ReplayRequest;
import com.dealshare.projectmanagement.realtime.api.ReplayResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RealTimeReplayService {

    private static final String PROJECT_STREAM_KEY = "realtime:project:%s:events";
    private static final String ISSUE_STREAM_KEY = "realtime:issue:%s:events";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RealTimeReplayService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public ReplayResponse replay(ReplayRequest request) {
        if (request.projectId() == null && (request.issueId() == null || request.issueId().isBlank())) {
            return new ReplayResponse(List.of());
        }
        String key = request.issueId() == null || request.issueId().isBlank()
                ? PROJECT_STREAM_KEY.formatted(request.projectId())
                : ISSUE_STREAM_KEY.formatted(request.issueId());
        List<String> rows = redis.opsForList().range(key, 0, -1);
        if (rows == null || rows.isEmpty()) {
            return new ReplayResponse(List.of());
        }

        List<RealTimeEventResponse> events = rows.stream().map(this::readEvent).toList();
        UUID lastSeen = request.lastSeenEventId();
        if (lastSeen == null) {
            return new ReplayResponse(events);
        }

        List<RealTimeEventResponse> missed = new ArrayList<>();
        boolean seen = false;
        for (RealTimeEventResponse event : events) {
            if (seen) {
                missed.add(event);
            } else if (event.eventId().equals(lastSeen)) {
                seen = true;
            }
        }
        return new ReplayResponse(seen ? missed : events);
    }

    private RealTimeEventResponse readEvent(String json) {
        try {
            return objectMapper.readValue(json, RealTimeEventResponse.class);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read replay event");
        }
    }
}
