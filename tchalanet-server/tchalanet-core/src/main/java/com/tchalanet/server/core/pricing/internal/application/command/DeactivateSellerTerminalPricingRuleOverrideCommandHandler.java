package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.pricing.api.command.DeactivateSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.error.PricingErrorCodes;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeactivateSellerTerminalPricingRuleOverrideCommandHandler
    implements CommandHandler<DeactivateSellerTerminalPricingRuleOverrideCommand, Void> {

  private final SellerTerminalOddsOverrideReaderPort reader;
  private final SellerTerminalOddsOverrideWriterPort writer;

  @Override
  @TchTx
  public Void handle(DeactivateSellerTerminalPricingRuleOverrideCommand c) {
    var override =
        reader
            .findById(c.overrideId())
            .orElseThrow(() -> ProblemRest.of(PricingErrorCodes.OVERRIDE_NOT_FOUND));
    writer.save(override.deactivate(c.actorId()));
    return null;
  }
}
