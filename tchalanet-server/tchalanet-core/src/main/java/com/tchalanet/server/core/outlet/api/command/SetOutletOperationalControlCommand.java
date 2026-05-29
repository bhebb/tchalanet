package com.tchalanet.server.core.outlet.api.command.block;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record SetOutletOperationalControlCommand(
    @NotNull OutletId outletId,
    @NotNull OutletOperationalControl control,
    boolean blocked,
    String reason,
    @NotNull UserId performedBy
) implements Command<Void> {}
