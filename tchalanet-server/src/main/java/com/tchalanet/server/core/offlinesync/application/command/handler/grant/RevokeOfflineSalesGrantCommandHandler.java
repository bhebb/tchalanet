package com.tchalanet.server.core.offlinesync.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.grant.RevokeOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.grant.RevokeOfflineSalesGrantResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrantStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RevokeOfflineSalesGrantCommandHandler
    implements CommandHandler<RevokeOfflineSalesGrantCommand, RevokeOfflineSalesGrantResult> {

  private final OfflineGrantWriterPort grantWriterPort;

  @Override
  @TchTx
  public RevokeOfflineSalesGrantResult handle(RevokeOfflineSalesGrantCommand command) {
    grantWriterPort.updateStatus(command.grantId(), OfflineSalesGrantStatus.REVOKED);
    return new RevokeOfflineSalesGrantResult(command.grantId());
  }
}

