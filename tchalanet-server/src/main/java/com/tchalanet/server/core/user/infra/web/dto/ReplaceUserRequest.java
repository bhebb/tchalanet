package com.tchalanet.server.core.user.infra.web.dto;

import java.util.UUID;

public record ReplaceUserRequest(
    UUID id,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    String avatarUrl,
    String status,
    String locale,
    String timeZone) {}
