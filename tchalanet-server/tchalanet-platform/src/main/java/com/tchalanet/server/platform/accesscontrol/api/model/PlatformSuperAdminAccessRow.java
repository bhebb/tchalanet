package com.tchalanet.server.platform.accesscontrol.api.model;

import java.time.Instant;
import java.util.UUID;

public record PlatformSuperAdminAccessRow(
    UUID userId,
    String username,
    String email,
    String displayName,
    String status,
    Instant assignedAt) {}
