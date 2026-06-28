package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.ResetSellerTerminalAccessCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class ResetSellerTerminalAccessCommandHandler
    implements CommandHandler<ResetSellerTerminalAccessCommand, Void> {

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;
    private final SellerTerminalIdentityProvisionPort identityProvision;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(ResetSellerTerminalAccessCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var reset = terminal.resetAccessMetadata();
        if (cmd.newCredential() != null && !cmd.newCredential().isBlank()) {
            if (identityProvision.hasExternalIdentity(cmd.terminalId())) {
                identityProvision.resetPin(cmd.terminalId(), cmd.tenantId(), cmd.newCredential());
            } else {
                identityProvision.provision(
                    cmd.terminalId(),
                    cmd.tenantId(),
                    terminal.terminalCode(),
                    terminal.displayName(),
                    cmd.newCredential());
            }
            if (terminal.status() == SellerTerminalStatus.PENDING) {
                reset = reset.activate(Instant.now(clock));
            }
        }
        writer.save(reset);
        return null;
    }
}
