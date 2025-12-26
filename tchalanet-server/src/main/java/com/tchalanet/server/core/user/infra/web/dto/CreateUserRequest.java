package com.tchalanet.server.core.user.infra.web.dto;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.Set;

public record CreateUserRequest(
    TenantId tenantIdInitiator,
    String email,
    String phone,
    String firstName,
    String lastName,
    String locale,
    boolean sendInvitation,
    Set<String> initialRoles
) {}
