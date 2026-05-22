package com.tchalanet.server.platform.identity.internal.web.admin.model;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 32) String phone,
    @Size(max = 120) String firstName,
    @Size(max = 120) String lastName
) {}
