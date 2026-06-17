package com.tchalanet.server.platform.identity.internal.web.model;

import com.tchalanet.server.common.types.id.UserId;

public record UserResponse(
    UserId id,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName) {}
