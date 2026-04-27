package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.domain.model.PosSession;
import java.math.BigDecimal;

public record OpenSessionCommand(
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId userId,
    BigDecimal openingFloat)
    implements Command<PosSession> {}
