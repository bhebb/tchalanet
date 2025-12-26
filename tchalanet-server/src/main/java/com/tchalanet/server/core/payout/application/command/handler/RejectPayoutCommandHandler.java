package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.application.command.model.RejectPayoutCommand;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RejectPayoutCommandHandler implements VoidCommandHandler<RejectPayoutCommand> {

  private final PayoutReaderPort reader;
  private final PayoutWriterPort writer;

  @Override
  @TchTx
  public void handle(RejectPayoutCommand cmd) {
    Payout payout =
        reader
            .findById(cmd.payoutId())
            .orElseThrow(() -> new IllegalArgumentException("Payout not found"));

    var when = cmd.rejectedAt() == null ? Instant.now() : cmd.rejectedAt();
    payout.reject(cmd.reason(), when);

    writer.save(payout);
  }
}
