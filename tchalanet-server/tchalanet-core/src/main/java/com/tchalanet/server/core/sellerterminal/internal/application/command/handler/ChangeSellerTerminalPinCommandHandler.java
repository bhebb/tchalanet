package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.ChangeSellerTerminalPinCommand;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import com.tchalanet.server.common.web.error.ProblemRestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ChangeSellerTerminalPinCommandHandler
    implements CommandHandler<ChangeSellerTerminalPinCommand, Void> {

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;
    private final SellerTerminalIdentityProvisionPort identityProvision;

    @Override
    @TchTx
    public Void handle(ChangeSellerTerminalPinCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());

        try {
            identityProvision.resetPin(cmd.terminalId(), cmd.tenantId(), cmd.newPin());
        } catch (IllegalStateException ex) {
            log.error("Firebase PIN change failed for sellerTerminal={}", cmd.terminalId().value(), ex);
            var pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
            pd.setTitle("Firebase error");
            pd.setDetail("Firebase PIN update failed");
            pd.setProperty("code", "seller_terminal.firebase_pin_reset_failed");
            throw new ProblemRestException(pd);
        }

        writer.save(terminal.changePin());
        return null;
    }
}
