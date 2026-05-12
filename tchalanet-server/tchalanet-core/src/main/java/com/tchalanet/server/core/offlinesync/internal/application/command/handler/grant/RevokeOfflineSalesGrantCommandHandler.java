package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.RevokeOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.RevokeOfflineSalesGrantResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;
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

