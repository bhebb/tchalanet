package com.tchalanet.server.core.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AppUser(
    UUID id,
    UUID keycloakId,
    UUID tenantId,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    String avatarUrl,
    UserStatus status,
    String locale,
    String timeZone,
    Instant lastLoginAt) {}
