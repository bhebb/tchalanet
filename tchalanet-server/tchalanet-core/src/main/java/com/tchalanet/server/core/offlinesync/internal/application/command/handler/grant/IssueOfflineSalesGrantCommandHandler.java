package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.api.command.IssueOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.IssueOfflineSalesGrantResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;
import com.tchalanet.server.core.session.api.query.PosOperationAction;
import com.tchalanet.server.core.session.api.query.ResolvePosOperationContextQuery;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class IssueOfflineSalesGrantCommandHandler
    implements CommandHandler<IssueOfflineSalesGrantCommand, IssueOfflineSalesGrantResult> {

  private final OfflineGrantWriterPort grantWriterPort;
  private final QueryBus queryBus;

  @Override
  @TchTx
  public IssueOfflineSalesGrantResult handle(IssueOfflineSalesGrantCommand command) {
    var posContext = queryBus.ask(new ResolvePosOperationContextQuery(
        command.tenantId(),
        command.sellerUserId(),
        command.operationalContext(),
        PosOperationAction.REQUEST_OFFLINE_GRANT));

    var grant = new OfflineSalesGrant(
        OfflineSalesGrantId.of(UUID.randomUUID()),
        command.tenantId(),
        posContext.terminalId(),
        posContext.outletId(),
        posContext.salesSessionId(),
        command.sellerUserId(),
        command.codeBatchId(),
        OfflineSalesGrantStatus.ACTIVE,
        Instant.now(),
        command.expiresAt());
    var id = grantWriterPort.save(grant);
    return new IssueOfflineSalesGrantResult(id, OfflineSalesGrantStatus.ACTIVE);
  }
}
