package com.dealshare.projectmanagement.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectManagementIntegrationTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID LEAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID VIEWER_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");
    private static final UUID ACTIVE_SPRINT_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("project_management_test")
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

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockBean private StringRedisTemplate redis;
    @MockBean private ValueOperations<String, String> valueOperations;

    private String leadToken;
    private String memberToken;
    private String viewerToken;

    @BeforeEach
    void resetDatabaseAndAuth() throws Exception {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(redis.expire(anyString(), any())).thenReturn(true);
        when(redis.delete(anyString())).thenReturn(true);

        flyway.clean();
        flyway.migrate();

        leadToken = token("lead@example.com");
        memberToken = token("member@example.com");
        viewerToken = token("viewer@example.com");
    }

    @Test
    void issueCrudCreatesUpdatesAndReadsIssueThroughBoard() throws Exception {
        JsonNode created = json(post(
                "/api/v1/projects/{projectId}/issues",
                PROJECT_ID
        ).header("Authorization", bearer(memberToken))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "type", "task",
                        "title", "Integration CRUD issue",
                        "description", "Created from integration test",
                        "priority", "medium",
                        "reporterId", MEMBER_ID,
                        "storyPoints", 3
                ))));

        String issueId = created.get("issueId").asText();
        long version = created.get("version").asLong();

        mvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "version", version,
                                "title", "Integration CRUD issue updated",
                                "priority", "high",
                                "storyPoints", 5
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(issueId))
                .andExpect(jsonPath("$.title").value("Integration CRUD issue updated"))
                .andExpect(jsonPath("$.priority").value("high"))
                .andExpect(jsonPath("$.storyPoints").value(5));

        mvc.perform(get("/api/v1/projects/{projectId}/board", PROJECT_ID)
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[*].issues[*].issueId").value(hasItem(issueId)));
    }

    @Test
    void boardQueryReturnsSeedWorkflowColumnsAndIssues() throws Exception {
        mvc.perform(get("/api/v1/projects/{projectId}/board", PROJECT_ID)
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.columns.length()").value(4))
                .andExpect(jsonPath("$.columns[*].name").value(hasItem("To Do")))
                .andExpect(jsonPath("$.columns[*].name").value(hasItem("In Progress")))
                .andExpect(jsonPath("$.columns[*].issues[*].issueId").value(hasItem("PROJ-1")))
                .andExpect(jsonPath("$.columns[*].issues[*].issueId").value(hasItem("PROJ-2")));
    }

    @Test
    void sprintStartAndCompleteUsesCarryOverResponse() throws Exception {
        JsonNode planned = json(post("/api/v1/projects/{projectId}/sprints", PROJECT_ID)
                .header("Authorization", bearer(leadToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "name", "Integration Sprint",
                        "goal", "Carry incomplete work",
                        "startDate", LocalDate.now().plusDays(15).toString(),
                        "endDate", LocalDate.now().plusDays(28).toString()
                ))));
        String targetSprintId = planned.get("id").asText();

        mvc.perform(post("/api/v1/sprints/{sprintId}/complete", ACTIVE_SPRINT_ID)
                        .header("Authorization", bearer(leadToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetSprintId", targetSprintId,
                                "carryOverIssueIds", java.util.List.of("PROJ-1")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprint.status").value("completed"))
                .andExpect(jsonPath("$.carriedOverIssues[*].issueId").value(hasItem("PROJ-1")))
                .andExpect(jsonPath("$.sprint.carriedOverStoryPoints").value(5));

        mvc.perform(post("/api/v1/sprints/{sprintId}/start", targetSprintId)
                        .header("Authorization", bearer(leadToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "startDate", LocalDate.now().plusDays(15).toString(),
                                "endDate", LocalDate.now().plusDays(28).toString()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void commentsMentionsAndNotificationsArePersisted() throws Exception {
        mvc.perform(post("/api/v1/issues/{issueId}/comments", "PROJ-1")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "authorId", MEMBER_ID,
                                "body", "Please review @lead@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value("PROJ-1"))
                .andExpect(jsonPath("$.mentions[*].userId").value(hasItem(LEAD_ID.toString())));

        mvc.perform(get("/api/v1/issues/{issueId}/comments", "PROJ-1")
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].body").value(hasItem("Please review @lead@example.com")));

        mvc.perform(get("/api/v1/users/{userId}/notifications", LEAD_ID)
                        .header("Authorization", bearer(leadToken))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("CommentAdded")))
                .andExpect(jsonPath("$[*].payload").value(hasItem(org.hamcrest.Matchers.containsString(LEAD_ID.toString()))));
    }

    @Test
    void activityFeedIncludesNewIssueAndCommentEvents() throws Exception {
        mvc.perform(post("/api/v1/issues/{issueId}/comments", "PROJ-1")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "authorId", MEMBER_ID,
                                "body", "Activity feed integration comment"
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/projects/{projectId}/activity", PROJECT_ID)
                        .header("Authorization", bearer(viewerToken))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].eventType").value(hasItem("CommentAdded")))
                .andExpect(jsonPath("$.items[*].eventType").value(hasItem("IssueCreated")));
    }

    @Test
    void searchFindsIssuesByTitleDescriptionAndCommentText() throws Exception {
        mvc.perform(post("/api/v1/issues/{issueId}/comments", "PROJ-2")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "authorId", MEMBER_ID,
                                "body", "needlecomment searchable discussion"
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/search")
                        .header("Authorization", bearer(viewerToken))
                .param("q", "Swagger")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].issueId").value(hasItem("PROJ-2")));

        mvc.perform(get("/api/v1/search")
                        .header("Authorization", bearer(viewerToken))
                        .param("q", "needlecomment")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].issueId").value(hasItem("PROJ-2")));
    }

    @Test
    void structuredSearchSupportsFiltersAndCursorPagination() throws Exception {
        JsonNode firstPage = json(get("/api/v1/search")
                .header("Authorization", bearer(viewerToken))
                .param("q", "status = \"In Progress\" AND assignee = \"Jane Smith\"")
                .param("limit", "1"));

        assertThat(firstPage.get("items")).hasSize(1);
        assertThat(firstPage.get("items").get(0).get("issueId").asText()).isEqualTo("PROJ-1");
    }

    @Test
    void customFieldsCanBeDefinedAndSetOnIssues() throws Exception {
        JsonNode definition = json(post("/api/v1/projects/{projectId}/custom-fields", PROJECT_ID)
                .header("Authorization", bearer(leadToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "fieldKey", "release_train",
                        "name", "Release Train",
                        "fieldType", "dropdown",
                        "options", "[\"R1\",\"R2\"]",
                        "required", false
                ))));

        mvc.perform(get("/api/v1/projects/{projectId}/custom-fields", PROJECT_ID)
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fieldKey").value(hasItem("release_train")));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/issues/{issueId}/custom-fields/{definitionId}",
                                "PROJ-1",
                                definition.get("id").asText()
                        )
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("value", "R1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldKey").value("release_train"))
                .andExpect(jsonPath("$.value").value("\"R1\""));
    }

    @Test
    void sprintAndCommentMutationsReplayIdempotentResponses() throws Exception {
        String sprintKey = UUID.randomUUID().toString();
        String sprintBody = json(Map.of(
                "name", "Idempotent Sprint",
                "goal", "Retry safely",
                "startDate", LocalDate.now().plusDays(30).toString(),
                "endDate", LocalDate.now().plusDays(44).toString()
        ));
        JsonNode firstSprint = json(post("/api/v1/projects/{projectId}/sprints", PROJECT_ID)
                .header("Authorization", bearer(leadToken))
                .header("Idempotency-Key", sprintKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sprintBody));
        JsonNode replayedSprint = json(post("/api/v1/projects/{projectId}/sprints", PROJECT_ID)
                .header("Authorization", bearer(leadToken))
                .header("Idempotency-Key", sprintKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sprintBody));
        assertThat(replayedSprint.get("id").asText()).isEqualTo(firstSprint.get("id").asText());

        String commentKey = UUID.randomUUID().toString();
        String commentBody = json(Map.of("authorId", MEMBER_ID, "body", "Idempotent comment"));
        JsonNode firstComment = json(post("/api/v1/issues/{issueId}/comments", "PROJ-1")
                .header("Authorization", bearer(memberToken))
                .header("Idempotency-Key", commentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody));
        JsonNode replayedComment = json(post("/api/v1/issues/{issueId}/comments", "PROJ-1")
                .header("Authorization", bearer(memberToken))
                .header("Idempotency-Key", commentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody));
        assertThat(replayedComment.get("id").asText()).isEqualTo(firstComment.get("id").asText());
    }

    @Test
    void sensitiveRoleChangesAreAudited() throws Exception {
        String adminToken = token("admin@example.com");
        mvc.perform(patch("/api/v1/projects/{projectId}/members/{userId}/role", PROJECT_ID, VIEWER_ID)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "member"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("member"));

        Integer auditRows = jdbcTemplate.queryForObject(
                "select count(*) from activity_log where project_id = ? and event_type = 'RoleChanged'",
                Integer.class,
                PROJECT_ID
        );
        assertThat(auditRows).isEqualTo(1);
    }

    @Test
    void simultaneousIssueUpdatesReturnConflictForStaleWriter() throws Exception {
        JsonNode created = createIssue("Concurrent update target", "To Do", MEMBER_ID);
        String issueId = created.get("issueId").asText();
        long version = created.get("version").asLong();

        List<Integer> statuses = runConcurrently(
                () -> mvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                                .header("Authorization", bearer(memberToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of(
                                        "version", version,
                                        "title", "Updated by user A"
                                ))))
                        .andReturn()
                        .getResponse()
                        .getStatus(),
                () -> mvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                                .header("Authorization", bearer(memberToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of(
                                        "version", version,
                                        "priority", "critical"
                                ))))
                        .andReturn()
                        .getResponse()
                        .getStatus()
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    void concurrentWipLimitedTransitionsDoNotExceedLimit() throws Exception {
        jdbcTemplate.update("""
                update workflow_statuses
                set wip_limit = 1
                where project_id = ? and name = 'In Review'
                """, PROJECT_ID);
        JsonNode first = createIssue("WIP candidate one", "In Progress", MEMBER_ID);
        JsonNode second = createIssue("WIP candidate two", "In Progress", MEMBER_ID);

        List<Integer> statuses = runConcurrently(
                () -> transition(first.get("issueId").asText(), first.get("version").asLong(), "In Review"),
                () -> transition(second.get("issueId").asText(), second.get("version").asLong(), "In Review")
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Long inReviewCount = jdbcTemplate.queryForObject("""
                select count(*)
                from issues issue
                join workflow_statuses status on status.id = issue.status_id
                where issue.project_id = ? and status.name = 'In Review'
                """, Long.class, PROJECT_ID);
        assertThat(inReviewCount).isEqualTo(1L);
    }

    @Test
    void concurrentSprintCompletionIsProtectedByAdvisoryLock() throws Exception {
        JsonNode planned = json(post("/api/v1/projects/{projectId}/sprints", PROJECT_ID)
                .header("Authorization", bearer(leadToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "name", "Advisory Lock Target",
                        "goal", "Receive carry-over once",
                        "startDate", LocalDate.now().plusDays(15).toString(),
                        "endDate", LocalDate.now().plusDays(28).toString()
                ))));
        String targetSprintId = planned.get("id").asText();

        List<Integer> statuses = runConcurrently(
                () -> completeSprint(ACTIVE_SPRINT_ID, targetSprintId),
                () -> completeSprint(ACTIVE_SPRINT_ID, targetSprintId)
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Long completedSprints = jdbcTemplate.queryForObject(
                "select count(*) from sprints where id = ? and status = 'completed'",
                Long.class,
                ACTIVE_SPRINT_ID
        );
        assertThat(completedSprints).isEqualTo(1L);
    }

    private JsonNode json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createIssue(String title, String status, UUID reporterId) throws Exception {
        return json(post("/api/v1/projects/{projectId}/issues", PROJECT_ID)
                .header("Authorization", bearer(memberToken))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "type", "task",
                        "title", title,
                        "description", title,
                        "status", status,
                        "priority", "medium",
                        "reporterId", reporterId,
                        "storyPoints", 1
                ))));
    }

    private int transition(String issueId, long version, String toStatus) throws Exception {
        return mvc.perform(post("/api/v1/issues/{issueId}/transitions", issueId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "version", version,
                                "toStatus", toStatus
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private int completeSprint(UUID sprintId, String targetSprintId) throws Exception {
        return mvc.perform(post("/api/v1/sprints/{sprintId}/complete", sprintId)
                        .header("Authorization", bearer(leadToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetSprintId", targetSprintId,
                                "carryOverIssueIds", List.of("PROJ-1")
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    @SafeVarargs
    private List<Integer> runConcurrently(Callable<Integer>... calls) throws Exception {
        var executor = Executors.newFixedThreadPool(calls.length);
        CountDownLatch ready = new CountDownLatch(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = java.util.Arrays.stream(calls)
                    .map(call -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return call.call();
                    }))
                    .toList();
            ready.await();
            start.countDown();
            return futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private String token(String email) throws Exception {
        JsonNode response = json(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
        return response.get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
