package com.tchalanet.server.platform.identity.internal.web.model;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @Size(max = 120) String firstName,
    @Size(max = 120) String lastName,
    @Size(max = 32) String phone,
    @Size(max = 16) String locale) {}
