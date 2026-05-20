package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @Email String email,
    @Size(max = 32) String phone,
    @Size(max = 120) String firstName,
    @Size(max = 120) String lastName,
    @NotNull TchRole role,
    OutletId outletId,
    TerminalId terminalId
) {}
