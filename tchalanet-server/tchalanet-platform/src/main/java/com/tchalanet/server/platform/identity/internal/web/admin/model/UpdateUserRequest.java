package com.tchalanet.server.platform.identity.internal.web.admin.model;

public record UpdateUserRequest(
    String email,
    String phone,
    String firstName,
    String lastName
) {}
