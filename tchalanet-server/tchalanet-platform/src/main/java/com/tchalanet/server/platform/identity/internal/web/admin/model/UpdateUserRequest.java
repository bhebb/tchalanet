package com.tchalanet.server.platform.identity.internal.web.admin.model;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
    @Size(max = 16) @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
    @Size(max = 120) String firstName,
    @Size(max = 120) String lastName
) {}
