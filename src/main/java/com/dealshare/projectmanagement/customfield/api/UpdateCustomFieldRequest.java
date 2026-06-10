package com.dealshare.projectmanagement.customfield.api;

public record UpdateCustomFieldRequest(
        String name,
        String fieldType,
        String options,
        Boolean required
) {
}
