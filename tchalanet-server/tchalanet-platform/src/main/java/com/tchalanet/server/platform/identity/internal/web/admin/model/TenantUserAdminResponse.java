package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record TenantUserAdminResponse(
    UserId id,
    String username,
    String email,
    String phone,
    String status,
    String role,
    String membershipStatus,
    ExternalIdentitySyncStatus externalIdentitySyncStatus,
    InvitationStatus invitationStatus,
    Instant createdAt,
    String firstName,
    String lastName,
    String displayName
) {}
