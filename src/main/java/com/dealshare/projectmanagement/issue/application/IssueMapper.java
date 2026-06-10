package com.dealshare.projectmanagement.issue.application;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.SprintEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.issue.api.IssueResponse;
import com.dealshare.projectmanagement.issue.api.SprintSummaryResponse;
import com.dealshare.projectmanagement.issue.api.UserSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {

    public IssueResponse toResponse(IssueEntity issue) {
        return new IssueResponse(
                issue.id(),
                issue.issueKey(),
                issue.projectId(),
                issue.type(),
                issue.title(),
                issue.description(),
                issue.status().name(),
                issue.priority(),
                issue.version(),
                user(issue.assignee()),
                user(issue.reporter()),
                sprint(issue.sprint()),
                issue.storyPoints(),
                issue.parent() == null ? null : issue.parent().issueKey(),
                issue.createdAt(),
                issue.updatedAt()
        );
    }

    private UserSummaryResponse user(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(user.id(), user.displayName());
    }

    private SprintSummaryResponse sprint(SprintEntity sprint) {
        if (sprint == null) {
            return null;
        }
        return new SprintSummaryResponse(sprint.id(), sprint.name(), sprint.status());
    }
}
