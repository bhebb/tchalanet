package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;

public record TenantUserAdminResponse(
    UserId id,
    String keycloakSub,
    String username,
    String email,
    String phone,
    String status,
    String role,
    String membershipStatus,
    OutletId outletId,
    TerminalId terminalId,
    KeycloakSyncStatus keycloakSyncStatus,
    InvitationStatus invitationStatus,
    Instant createdAt,
    String firstName,
    String lastName,
    String displayName
) {}
