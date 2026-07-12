package com.tchalanet.server.platform.identity.internal.model;

import com.tchalanet.server.common.types.id.ExternalUserSubject;
import com.tchalanet.server.common.types.id.UserId;

public record UserRow(
    UserId id,
    ExternalUserSubject externalSubject,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String status) {}
