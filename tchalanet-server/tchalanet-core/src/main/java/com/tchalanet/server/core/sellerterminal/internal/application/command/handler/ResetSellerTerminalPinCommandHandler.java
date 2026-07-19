package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sellerterminal.api.command.ResetSellerTerminalPinCommand;
import com.tchalanet.server.core.sellerterminal.api.error.SellerTerminalErrorCodes;
import com.tchalanet.server.core.sellerterminal.api.model.ResetSellerTerminalPinView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
      throw ProblemRest.of(SellerTerminalErrorCodes.STATUS_TRANSITION_INVALID);
    }

    if (!identityProvision.hasExternalIdentity(cmd.terminalId())) {
      throw ProblemRest.of(SellerTerminalErrorCodes.IDENTITY_NOT_BOUND);
    }

    var pin = generatePin();
    var now = Instant.now(clock);

    try {
      identityProvision.resetPin(cmd.terminalId(), cmd.tenantId(), pin);
    } catch (IllegalStateException ex) {
      log.error("Firebase PIN reset failed for sellerTerminal={}", cmd.terminalId().value(), ex);
      throw ProblemRest.of(SellerTerminalErrorCodes.PIN_RESET_UNAVAILABLE, java.util.Map.of(), ex);
    }

    var updated = terminal.resetPin(now);
    if (terminal.status() == SellerTerminalStatus.PENDING) {
      updated = updated.activate(now);
    }
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
