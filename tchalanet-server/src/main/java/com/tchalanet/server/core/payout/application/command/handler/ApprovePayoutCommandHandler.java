package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.application.command.model.ApprovePayoutCommand;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApprovePayoutCommandHandler implements VoidCommandHandler<ApprovePayoutCommand> {

  private final PayoutReaderPort payoutReaderPort;
  private final PayoutWriterPort payoutWriterPort;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(ApprovePayoutCommand command) {
    Optional<Payout> opt = payoutReaderPort.findById(command.payoutId());
    if (opt.isEmpty()) throw new IllegalStateException("Payout not found: " + command.payoutId());
    Payout payout = opt.get();

    // ensure same tenant
    if (!payout.getTenantId().equals(command.tenantId())) {
      throw new IllegalStateException("Tenant mismatch for payout approval");
    }

    if (payout.getStatus() != PayoutStatus.REQUESTED) {
      throw new IllegalStateException("Only REQUESTED payouts can be approved");
    }

    payout.approve(Instant.now(clock));
    payoutWriterPort.save(payout);
  }
}
