package com.tchalanet.server.core.terminal.internal.application.command.handler.sellerterminal;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.command.ResetSellerTerminalAccessCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResetSellerTerminalAccessCommandHandler
    implements CommandHandler<ResetSellerTerminalAccessCommand, Void> {

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;
    private final SellerTerminalIdentityProvisionPort identityProvision;

    @Override
    @TchTx
    public Void handle(ResetSellerTerminalAccessCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var reset = terminal.resetAccessMetadata();
        writer.save(reset);
        if (cmd.newCredential() != null && !cmd.newCredential().isBlank()) {
            identityProvision.resetPin(cmd.terminalId(), cmd.tenantId(), cmd.newCredential());
        }
        return null;
    }
}
