package com.tchalanet.server.core.user.infra.web.dto;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;

/** Minimal view returned by /api/v1/profile/bootstrap */
public record UserResponse(
    UserId id,
    UUID keycloakId,
    TenantId tenantId,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName) {}
