package com.tchalanet.server.core.terminal.internal.application.command.handler.sellerterminal;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.terminal.api.command.CreateSellerTerminalCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class CreateSellerTerminalCommandHandler
    implements CommandHandler<CreateSellerTerminalCommand, SellerTerminalId> {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("15.00");

    private final SellerTerminalWriterPort writer;
    private final IdGenerator idGenerator;
    private final SellerTerminalIdentityProvisionPort identityProvision;

    @Override
    @TchTx
    public SellerTerminalId handle(CreateSellerTerminalCommand cmd) {
        var id = SellerTerminalId.of(idGenerator.newUuid());
        var rate = cmd.commissionRate() != null ? cmd.commissionRate() : DEFAULT_COMMISSION_RATE;

        var terminal = SellerTerminal.createPending(
            id,
            cmd.tenantId(),
            cmd.terminalCode(),
            cmd.displayName(),
            cmd.firstName(),
            cmd.lastName(),
            cmd.phoneNumber(),
            cmd.addressId(),
            cmd.outletId(),
            rate);

        writer.save(terminal);
        if (cmd.initialPin() != null && !cmd.initialPin().isBlank()) {
            identityProvision.provision(
                id, cmd.tenantId(), cmd.terminalCode(), cmd.displayName(), cmd.initialPin());
        }
        return id;
    }
}
