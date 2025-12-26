package com.tchalanet.server.core.user.infra.web.dto;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record MeResponse(
    UUID id,
    UUID keycloakId,
    UUID tenantId,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    boolean isNew
) {}

