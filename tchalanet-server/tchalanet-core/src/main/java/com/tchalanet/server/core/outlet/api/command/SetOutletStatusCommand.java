package com.tchalanet.server.core.outlet.api.command.lifecycle;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import jakarta.validation.constraints.NotNull;

public record SetOutletStatusCommand(
    @NotNull OutletId outletId,
    @NotNull OutletStatus status,
    String reason,
    @NotNull UserId performedBy
) implements Command<Void> {}
