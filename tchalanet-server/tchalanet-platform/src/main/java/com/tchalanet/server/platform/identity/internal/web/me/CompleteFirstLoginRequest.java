package com.tchalanet.server.platform.identity.internal.web.me;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record CompleteFirstLoginRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    String phoneNumber,
    @AssertTrue boolean passwordChanged) {}
