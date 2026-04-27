package com.tchalanet.server.features.tenantadmin.users.model;

public record UpdateUserRequest(
    String email,
    String phone,
    String firstName,
    String lastName
) {}
