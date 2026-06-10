package com.dealshare.projectmanagement.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank @Email String email) {
}
