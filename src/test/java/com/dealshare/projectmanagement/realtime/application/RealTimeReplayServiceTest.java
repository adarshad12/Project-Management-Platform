package com.dealshare.projectmanagement.realtime.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.realtime.api.RealTimeEventResponse;
import com.dealshare.projectmanagement.realtime.api.ReplayRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RealTimeReplayServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ListOperations<String, String> listOperations;

    @Test
    void replaysOnlyEventsAfterLastSeenEvent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        RealTimeEventResponse first = new RealTimeEventResponse(firstId, "issue_updated", projectId, "PROJ-1", null, "{}", Instant.now());
        RealTimeEventResponse second = new RealTimeEventResponse(secondId, "comment_added", projectId, "PROJ-1", null, "{}", Instant.now());
        when(redis.opsForList()).thenReturn(listOperations);
        when(listOperations.range("realtime:issue:PROJ-1:events", 0, -1))
                .thenReturn(List.of(objectMapper.writeValueAsString(first), objectMapper.writeValueAsString(second)));

        RealTimeReplayService service = new RealTimeReplayService(redis, objectMapper);

        assertThat(service.replay(new ReplayRequest(null, "PROJ-1", firstId)).events())
                .containsExactly(second);
    }
}
