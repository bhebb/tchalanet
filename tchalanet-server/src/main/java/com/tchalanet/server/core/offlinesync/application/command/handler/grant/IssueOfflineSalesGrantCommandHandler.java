package com.tchalanet.server.core.offlinesync.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.application.command.model.grant.IssueOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.grant.IssueOfflineSalesGrantResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrantStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class IssueOfflineSalesGrantCommandHandler
    implements CommandHandler<IssueOfflineSalesGrantCommand, IssueOfflineSalesGrantResult> {

  private final OfflineGrantWriterPort grantWriterPort;

  @Override
  @TchTx
  public IssueOfflineSalesGrantResult handle(IssueOfflineSalesGrantCommand command) {
    var grant = new OfflineSalesGrant(
        OfflineSalesGrantId.of(UUID.randomUUID()),
        command.tenantId(),
        command.terminalId(),
        command.outletId(),
        command.salesSessionId(),
        command.sellerUserId(),
        command.codeBatchId(),
        OfflineSalesGrantStatus.ACTIVE,
        Instant.now(),
        command.expiresAt());
    var id = grantWriterPort.save(grant);
    return new IssueOfflineSalesGrantResult(id, OfflineSalesGrantStatus.ACTIVE);
  }
}

