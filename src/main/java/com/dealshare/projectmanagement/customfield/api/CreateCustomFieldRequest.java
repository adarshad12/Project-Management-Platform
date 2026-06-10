package com.dealshare.projectmanagement.customfield.api;

import jakarta.validation.constraints.NotBlank;

public record CreateCustomFieldRequest(
        @NotBlank String fieldKey,
        @NotBlank String name,
        @NotBlank String fieldType,
        String options,
        boolean required
) {
}
