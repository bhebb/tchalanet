package com.tchalanet.server.core.tenantuser.infra.web.admin.model;

public record UpdateUserRequest(
    String email,
    String phone,
    String firstName,
    String lastName
) {}
