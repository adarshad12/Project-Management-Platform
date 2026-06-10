package com.dealshare.projectmanagement.customfield.api;

import com.dealshare.projectmanagement.customfield.application.CustomFieldService;
import com.dealshare.projectmanagement.security.ProjectRole;
import com.dealshare.projectmanagement.security.RbacService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomFieldController {

    private final CustomFieldService customFields;
    private final RbacService rbac;

    public CustomFieldController(CustomFieldService customFields, RbacService rbac) {
        this.customFields = customFields;
        this.rbac = rbac;
    }

    @GetMapping("/projects/{projectId}/custom-fields")
    List<CustomFieldDefinitionResponse> listDefinitions(@PathVariable UUID projectId) {
        rbac.requireProjectRole(projectId, ProjectRole.VIEWER);
        return customFields.listDefinitions(projectId);
    }

    @PostMapping("/projects/{projectId}/custom-fields")
    CustomFieldDefinitionResponse createDefinition(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateCustomFieldRequest request
    ) {
        rbac.requireProjectRole(projectId, ProjectRole.PROJECT_LEAD);
        return customFields.createDefinition(projectId, request);
    }

    @PatchMapping("/projects/{projectId}/custom-fields/{definitionId}")
    CustomFieldDefinitionResponse updateDefinition(
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId,
            @RequestBody UpdateCustomFieldRequest request
    ) {
        rbac.requireProjectRole(projectId, ProjectRole.PROJECT_LEAD);
        return customFields.updateDefinition(projectId, definitionId, request);
    }

    @DeleteMapping("/projects/{projectId}/custom-fields/{definitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDefinition(@PathVariable UUID projectId, @PathVariable UUID definitionId) {
        rbac.requireProjectRole(projectId, ProjectRole.PROJECT_LEAD);
        customFields.deleteDefinition(projectId, definitionId);
    }

    @GetMapping("/issues/{issueId}/custom-fields")
    List<CustomFieldValueResponse> listIssueValues(@PathVariable String issueId) {
        rbac.requireIssueRole(issueId, ProjectRole.VIEWER);
        return customFields.listIssueValues(issueId);
    }

    @PutMapping("/issues/{issueId}/custom-fields/{definitionId}")
    CustomFieldValueResponse setIssueValue(
            @PathVariable String issueId,
            @PathVariable UUID definitionId,
            @Valid @RequestBody SetCustomFieldValueRequest request
    ) {
        rbac.requireIssueRole(issueId, ProjectRole.MEMBER);
        return customFields.setIssueValue(issueId, definitionId, request);
    }
}
