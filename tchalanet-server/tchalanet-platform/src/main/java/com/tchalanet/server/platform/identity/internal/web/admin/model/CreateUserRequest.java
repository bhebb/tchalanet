package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;

public record CreateUserRequest(
    String email,
    String phone,
    String firstName,
    String lastName,
    TchRole role,
    OutletId outletId,
    TerminalId terminalId
) {}
