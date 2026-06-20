package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.exception.TchConflictException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.ResetSellerTerminalPinCommand;
import com.tchalanet.server.core.sellerterminal.api.model.ResetSellerTerminalPinView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import com.tchalanet.server.common.web.error.ProblemRestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ResetSellerTerminalPinCommandHandler
    implements CommandHandler<ResetSellerTerminalPinCommand, ResetSellerTerminalPinView> {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;
    private final SellerTerminalIdentityProvisionPort identityProvision;
    private final Clock clock;

    @Override
    @TchTx
    public ResetSellerTerminalPinView handle(ResetSellerTerminalPinCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());

        if (terminal.status() == SellerTerminalStatus.DISABLED) {
            throw new TchConflictException(
                "seller_terminal.archived",
                "Seller terminal is disabled and cannot have its PIN reset");
        }

        if (!identityProvision.hasExternalIdentity(cmd.terminalId())) {
            throw new TchConflictException(
                "seller_terminal.identity_not_bound",
                "Seller terminal has no bound Firebase identity");
        }

        var pin = generatePin();
        var now = Instant.now(clock);

        try {
            identityProvision.resetPin(cmd.terminalId(), cmd.tenantId(), pin);
        } catch (IllegalStateException ex) {
            log.error("Firebase PIN reset failed for sellerTerminal={}", cmd.terminalId().value(), ex);
            var pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
            pd.setTitle("Firebase error");
            pd.setDetail("Firebase PIN reset failed");
            pd.setProperty("code", "seller_terminal.firebase_pin_reset_failed");
            throw new ProblemRestException(pd);
        }

        var updated = terminal.resetPin(now);
        writer.save(updated);

        return new ResetSellerTerminalPinView(
            terminal.id().value(),
            terminal.terminalCode(),
            pin,
            updated.mustChangePin(),
            updated.pinResetAt());
    }

    private static String generatePin() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
