package com.tchalanet.server.platform.accesscontrol.api.model.view;

import java.time.Instant;
import java.util.UUID;

public record UserPermissionOverrideView(
    UUID id,
    String permissionCode,
    String effect,
    String reason,
    Instant createdAt) {}
