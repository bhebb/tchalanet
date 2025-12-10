package com.tchalanet.server.core.user.application.query.model;

import java.util.UUID;

public record SearchUsersQuery(
    UUID tenantId,
    String text,
    int page,
    int size) {}

