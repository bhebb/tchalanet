package com.tchalanet.server.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AppUser(
    UUID id,
    UUID tenantId,
    String username,
    String email,
    String displayName,
    String locale,
    Instant lastLoginAt) {}
