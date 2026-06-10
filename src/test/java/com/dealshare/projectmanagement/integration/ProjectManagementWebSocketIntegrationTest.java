package com.dealshare.projectmanagement.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dealshare.projectmanagement.realtime.api.RealTimeEventResponse;
import com.dealshare.projectmanagement.realtime.api.ReplayRequest;
import com.dealshare.projectmanagement.realtime.api.ReplayResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectManagementWebSocketIntegrationTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("project_management_websocket_test")
            .withUsername("pm_user")
            .withPassword("pm_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort private int port;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private Flyway flyway;

    @MockBean private StringRedisTemplate redis;
    @MockBean private ValueOperations<String, String> valueOperations;
    @MockBean private ListOperations<String, String> listOperations;
    @MockBean private HashOperations<String, Object, Object> hashOperations;

    private final Map<String, CopyOnWriteArrayList<String>> replayBuffers = new ConcurrentHashMap<>();
    private String memberToken;

    @BeforeEach
    void resetDatabaseAndRedis() throws Exception {
        replayBuffers.clear();
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(redis.opsForList()).thenReturn(listOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(redis.delete(anyString())).thenReturn(true);
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            CopyOnWriteArrayList<String> values = replayBuffers.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
            values.add(value);
            return (long) values.size();
        }).when(listOperations).rightPush(anyString(), anyString());
        doAnswer(invocation -> null).when(listOperations).trim(anyString(), anyLong(), anyLong());
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return List.copyOf(replayBuffers.getOrDefault(key, new CopyOnWriteArrayList<>()));
        });

        flyway.clean();
        flyway.migrate();
        memberToken = token("member@example.com");
    }

    @Test
    void clientsReceiveBoardUpdates() throws Exception {
        StompSession session = connect(memberToken);
        BlockingQueue<RealTimeEventResponse> events = new LinkedBlockingQueue<>();
        session.subscribe("/topic/projects/" + PROJECT_ID + "/board", handler(RealTimeEventResponse.class, events));

        updateIssue("PROJ-2", 0L, Map.of("title", "WebSocket board update"));

        RealTimeEventResponse event = events.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.type()).isEqualTo("issue_updated");
        assertThat(event.projectId()).isEqualTo(PROJECT_ID);
        assertThat(event.issueId()).isEqualTo("PROJ-2");
        session.disconnect();
    }

    @Test
    void reconnectReplaysMissedEvents() throws Exception {
        StompSession firstSession = connect(memberToken);
        BlockingQueue<RealTimeEventResponse> boardEvents = new LinkedBlockingQueue<>();
        firstSession.subscribe("/topic/projects/" + PROJECT_ID + "/board", handler(RealTimeEventResponse.class, boardEvents));

        updateIssue("PROJ-2", 0L, Map.of("title", "First replay baseline"));
        RealTimeEventResponse firstEvent = boardEvents.poll(5, TimeUnit.SECONDS);
        assertThat(firstEvent).isNotNull();
        firstSession.disconnect();

        updateIssue("PROJ-2", 1L, Map.of("priority", "high"));

        StompSession reconnect = connect(memberToken);
        BlockingQueue<ReplayResponse> replayResponses = new LinkedBlockingQueue<>();
        reconnect.subscribe("/user/queue/replay", handler(ReplayResponse.class, replayResponses));
        reconnect.send("/app/realtime/replay", new ReplayRequest(PROJECT_ID, null, firstEvent.eventId()));

        ReplayResponse replay = replayResponses.poll(5, TimeUnit.SECONDS);
        assertThat(replay).isNotNull();
        assertThat(replay.events())
                .extracting(RealTimeEventResponse::type)
                .containsExactly("issue_updated");
        assertThat(replay.events())
                .extracting(RealTimeEventResponse::issueId)
                .containsExactly("PROJ-2");
        reconnect.disconnect();
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);
        return client.connectAsync(
                "ws://localhost:" + port + "/ws?access_token=" + token,
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);
    }

    private <T> StompFrameHandler handler(Class<T> payloadType, BlockingQueue<T> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(payloadType.cast(payload));
            }
        };
    }

    private void updateIssue(String issueId, long version, Map<String, Object> fields) throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>(fields);
        body.put("version", version);
        mvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk());
    }

    private String token(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
