package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TerminalId;

public record CreateUserRequest(
    String email,
    String phone,
    String firstName,
    String lastName,
    TchRole role, // ou enum RoleKey
    OutletId outletId,
    TerminalId terminalId
) {}
