package com.dealshare.projectmanagement.customfield.application;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.customfield.api.CreateCustomFieldRequest;
import com.dealshare.projectmanagement.customfield.api.CustomFieldDefinitionResponse;
import com.dealshare.projectmanagement.customfield.api.CustomFieldValueResponse;
import com.dealshare.projectmanagement.customfield.api.SetCustomFieldValueRequest;
import com.dealshare.projectmanagement.customfield.api.UpdateCustomFieldRequest;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.CustomFieldDefinitionEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueCustomFieldValueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueCustomFieldValueId;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IssueEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.ProjectEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.CustomFieldDefinitionJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueCustomFieldValueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IssueJpaRepository;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.ProjectJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomFieldService {

    private final ProjectJpaRepository projects;
    private final IssueJpaRepository issues;
    private final CustomFieldDefinitionJpaRepository definitions;
    private final IssueCustomFieldValueJpaRepository values;
    private final ObjectMapper objectMapper;

    public CustomFieldService(
            ProjectJpaRepository projects,
            IssueJpaRepository issues,
            CustomFieldDefinitionJpaRepository definitions,
            IssueCustomFieldValueJpaRepository values,
            ObjectMapper objectMapper
    ) {
        this.projects = projects;
        this.issues = issues;
        this.definitions = definitions;
        this.values = values;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionResponse> listDefinitions(UUID projectId) {
        project(projectId);
        return definitions.findByProjectId(projectId).stream().map(this::toDefinitionResponse).toList();
    }

    @Transactional
    public CustomFieldDefinitionResponse createDefinition(UUID projectId, CreateCustomFieldRequest request) {
        ProjectEntity project = project(projectId);
        String fieldType = normalizeFieldType(request.fieldType());
        validateOptions(fieldType, request.options());
        CustomFieldDefinitionEntity definition = definitions.save(new CustomFieldDefinitionEntity(
                UUID.randomUUID(),
                project,
                normalizeFieldKey(request.fieldKey()),
                request.name(),
                fieldType,
                request.options() == null || request.options().isBlank() ? "[]" : request.options(),
                request.required(),
                Instant.now()
        ));
        return toDefinitionResponse(definition);
    }

    @Transactional
    public CustomFieldDefinitionResponse updateDefinition(UUID projectId, UUID definitionId, UpdateCustomFieldRequest request) {
        project(projectId);
        CustomFieldDefinitionEntity definition = definition(definitionId);
        ensureSameProject(projectId, definition);
        String fieldType = request.fieldType() == null ? null : normalizeFieldType(request.fieldType());
        String options = request.options();
        validateOptions(fieldType == null ? definition.fieldType() : fieldType, options == null ? definition.options() : options);
        definition.update(request.name(), fieldType, options, request.required());
        return toDefinitionResponse(definitions.save(definition));
    }

    @Transactional
    public void deleteDefinition(UUID projectId, UUID definitionId) {
        project(projectId);
        CustomFieldDefinitionEntity definition = definition(definitionId);
        ensureSameProject(projectId, definition);
        definitions.delete(definition);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldValueResponse> listIssueValues(String issueId) {
        IssueEntity issue = issue(issueId);
        return values.findByIssueId(issue.id()).stream().map(this::toValueResponse).toList();
    }

    @Transactional
    public CustomFieldValueResponse setIssueValue(String issueId, UUID definitionId, SetCustomFieldValueRequest request) {
        IssueEntity issue = issue(issueId);
        CustomFieldDefinitionEntity definition = definition(definitionId);
        ensureSameProject(issue.projectId(), definition);
        String jsonValue = writeJson(request.value());
        validateValue(definition, jsonValue);
        IssueCustomFieldValueId id = new IssueCustomFieldValueId(issue.id(), definition.id());
        IssueCustomFieldValueEntity value = values.findById(id)
                .map(existing -> {
                    existing.updateValue(jsonValue);
                    return existing;
                })
                .orElseGet(() -> new IssueCustomFieldValueEntity(id, issue, definition, jsonValue, Instant.now()));
        return toValueResponse(values.save(value));
    }

    private void validateValue(CustomFieldDefinitionEntity definition, String jsonValue) {
        try {
            JsonNode value = objectMapper.readTree(jsonValue);
            switch (definition.fieldType()) {
                case "text" -> {
                    if (!value.isTextual()) {
                        throw validation("Custom field " + definition.fieldKey() + " expects a text value");
                    }
                }
                case "number" -> {
                    if (!value.isNumber()) {
                        throw validation("Custom field " + definition.fieldKey() + " expects a number value");
                    }
                }
                case "date" -> {
                    if (!value.isTextual()) {
                        throw validation("Custom field " + definition.fieldKey() + " expects an ISO date string");
                    }
                    LocalDate.parse(value.asText());
                }
                case "dropdown" -> validateDropdownValue(definition, value);
                default -> throw validation("Unsupported custom field type: " + definition.fieldType());
            }
        } catch (JsonProcessingException | java.time.format.DateTimeParseException exception) {
            throw validation("Invalid custom field value for " + definition.fieldKey());
        }
    }

    private void validateDropdownValue(CustomFieldDefinitionEntity definition, JsonNode value) throws JsonProcessingException {
        if (!value.isTextual()) {
            throw validation("Custom field " + definition.fieldKey() + " expects a dropdown string value");
        }
        JsonNode options = objectMapper.readTree(definition.options());
        if (!options.isArray()) {
            throw validation("Dropdown custom field options must be a JSON array");
        }
        for (JsonNode option : options) {
            if (option.asText().equals(value.asText())) {
                return;
            }
        }
        throw validation("Value is not an allowed option for " + definition.fieldKey());
    }

    private void validateOptions(String fieldType, String optionsJson) {
        if (!"dropdown".equals(fieldType)) {
            return;
        }
        try {
            JsonNode options = objectMapper.readTree(optionsJson == null || optionsJson.isBlank() ? "[]" : optionsJson);
            if (!options.isArray() || options.isEmpty()) {
                throw validation("Dropdown custom fields require a non-empty JSON array of options");
            }
        } catch (JsonProcessingException exception) {
            throw validation("Dropdown custom field options must be valid JSON");
        }
    }

    private CustomFieldDefinitionResponse toDefinitionResponse(CustomFieldDefinitionEntity definition) {
        return new CustomFieldDefinitionResponse(
                definition.id(),
                definition.projectId(),
                definition.fieldKey(),
                definition.name(),
                definition.fieldType(),
                definition.options(),
                definition.required(),
                definition.createdAt()
        );
    }

    private CustomFieldValueResponse toValueResponse(IssueCustomFieldValueEntity value) {
        CustomFieldDefinitionEntity definition = value.fieldDefinition();
        return new CustomFieldValueResponse(
                value.issue().issueKey(),
                definition.id(),
                definition.fieldKey(),
                definition.name(),
                definition.fieldType(),
                value.value(),
                value.updatedAt()
        );
    }

    private ProjectEntity project(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Project not found"));
    }

    private IssueEntity issue(String issueId) {
        try {
            return issues.findById(UUID.fromString(issueId))
                    .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Issue not found"));
        } catch (IllegalArgumentException ignored) {
            return issues.findByIssueKey(issueId)
                    .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Issue not found"));
        }
    }

    private CustomFieldDefinitionEntity definition(UUID definitionId) {
        return definitions.findById(definitionId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Custom field definition not found"));
    }

    private void ensureSameProject(UUID projectId, CustomFieldDefinitionEntity definition) {
        if (!definition.projectId().equals(projectId)) {
            throw validation("Custom field definition belongs to another project");
        }
    }

    private String normalizeFieldType(String fieldType) {
        String normalized = fieldType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("text", "number", "dropdown", "date").contains(normalized)) {
            throw validation("Unsupported custom field type: " + fieldType);
        }
        return normalized;
    }

    private String normalizeFieldKey(String fieldKey) {
        return fieldKey.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw validation("Custom field value must be JSON serializable");
        }
    }

    private DomainException validation(String message) {
        return new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
