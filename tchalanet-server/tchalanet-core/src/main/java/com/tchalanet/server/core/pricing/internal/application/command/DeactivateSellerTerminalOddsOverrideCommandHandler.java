package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.command.DeactivateSellerTerminalOddsOverrideCommand;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeactivateSellerTerminalOddsOverrideCommandHandler
    implements CommandHandler<DeactivateSellerTerminalOddsOverrideCommand, Void> {

    private final SellerTerminalOddsOverrideReaderPort reader;
    private final SellerTerminalOddsOverrideWriterPort writer;

    @Override
    @TchTx
    public Void handle(DeactivateSellerTerminalOddsOverrideCommand c) {
        var override = reader.findById(c.overrideId())
            .orElseThrow(() -> new IllegalArgumentException("Override not found: " + c.overrideId()));
        writer.save(override.deactivate(c.actorId()));
        return null;
    }
}
