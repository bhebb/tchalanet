package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.sellerterminal.api.command.CreateSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class CreateSellerTerminalCommandHandler
    implements CommandHandler<CreateSellerTerminalCommand, SellerTerminalId> {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("15.00");

    private final SellerTerminalWriterPort writer;
    private final IdGenerator idGenerator;
    private final SellerTerminalIdentityProvisionPort identityProvision;
    private final Clock clock;

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
            cmd.email(),
            cmd.phoneNumber(),
            cmd.addressId(),
            rate);

        if (cmd.initialPin() != null && !cmd.initialPin().isBlank()) {
            identityProvision.provision(
                id, cmd.tenantId(), cmd.terminalCode(), cmd.displayName(), cmd.initialPin());
            terminal = terminal.activate(Instant.now(clock));
        }
        writer.save(terminal);
        return id;
    }
}
