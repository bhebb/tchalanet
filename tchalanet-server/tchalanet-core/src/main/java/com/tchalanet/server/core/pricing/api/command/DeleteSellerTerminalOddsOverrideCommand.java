package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.UserId;

public record DeleteSellerTerminalOddsOverrideCommand(
    SellerTerminalOddsOverrideId overrideId,
    UserId actorId
) implements Command<Void> {}
