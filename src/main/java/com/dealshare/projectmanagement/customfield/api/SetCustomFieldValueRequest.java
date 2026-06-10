package com.dealshare.projectmanagement.customfield.api;

import jakarta.validation.constraints.NotNull;

public record SetCustomFieldValueRequest(@NotNull Object value) {
}
