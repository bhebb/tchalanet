package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateSellerTerminalCommandHandler
    implements CommandHandler<UpdateSellerTerminalCommand, Void> {

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;

    @Override
    @TchTx
    public Void handle(UpdateSellerTerminalCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());

        var updated = terminal.updateProfile(
            cmd.displayName(),
            cmd.firstName(),
            cmd.lastName(),
            cmd.email(),
            cmd.phoneNumber(),
            cmd.addressId());

        if (cmd.commissionRate() != null
            && cmd.commissionRate().compareTo(terminal.commissionRate()) != 0) {
            updated = updated.updateCommissionRate(cmd.commissionRate());
        }

        writer.save(updated);
        return null;
    }
}
