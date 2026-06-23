package com.tchalanet.server.platform.identity.internal.web.me;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String newPassword) {}
