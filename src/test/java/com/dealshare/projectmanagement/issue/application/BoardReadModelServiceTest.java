package com.dealshare.projectmanagement.issue.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dealshare.projectmanagement.infrastructure.persistence.repository.BoardIssueReadModelJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.WorkflowStatusJpaRepository;
import com.dealshare.projectmanagement.issue.api.BoardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class BoardReadModelServiceTest {

    @Mock private BoardIssueReadModelJpaRepository readModel;
    @Mock private IssueJpaRepository issues;
    @Mock private WorkflowStatusJpaRepository statuses;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    void cachedBoardAvoidsDatabaseReadModelQuery() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        UUID projectId = UUID.randomUUID();
        BoardResponse cachedBoard = new BoardResponse(projectId, List.of());
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("board:project:" + projectId)).thenReturn(objectMapper.writeValueAsString(cachedBoard));
        BoardReadModelService service = new BoardReadModelService(
                readModel,
                issues,
                statuses,
                redis,
                objectMapper,
                new SimpleMeterRegistry()
        );

        assertThat(service.board(projectId)).isEqualTo(cachedBoard);
        verifyNoInteractions(readModel, issues, statuses);
    }
}
