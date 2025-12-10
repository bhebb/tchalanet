package com.tchalanet.server.core.user.infra.web.dto;

import java.util.UUID;

/** Minimal view returned by /api/v1/profile/bootstrap */
public record UserResponse(
    UUID id,
    UUID keycloakId,
    UUID tenantId,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName
) {}

