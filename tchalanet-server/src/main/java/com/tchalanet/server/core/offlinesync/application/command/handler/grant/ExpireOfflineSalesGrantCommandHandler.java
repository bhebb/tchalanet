package com.tchalanet.server.core.offlinesync.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.grant.ExpireOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.grant.ExpireOfflineSalesGrantResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrantStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExpireOfflineSalesGrantCommandHandler
    implements CommandHandler<ExpireOfflineSalesGrantCommand, ExpireOfflineSalesGrantResult> {

  private final OfflineGrantWriterPort grantWriterPort;

  @Override
  @TchTx
  public ExpireOfflineSalesGrantResult handle(ExpireOfflineSalesGrantCommand command) {
    grantWriterPort.updateStatus(command.grantId(), OfflineSalesGrantStatus.EXPIRED);
    return new ExpireOfflineSalesGrantResult(command.grantId());
  }
}

