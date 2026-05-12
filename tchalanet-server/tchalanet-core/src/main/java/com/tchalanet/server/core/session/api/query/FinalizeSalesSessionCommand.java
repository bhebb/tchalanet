package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record FinalizeSalesSessionCommand(
    @NotNull SalesSessionId salesSessionId,
    String reason,
    @NotNull UserId performedBy
) implements Command<Void> {}
