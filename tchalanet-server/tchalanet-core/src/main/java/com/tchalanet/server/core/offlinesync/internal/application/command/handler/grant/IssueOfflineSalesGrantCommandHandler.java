package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.offlinesync.api.command.IssueOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.IssueOfflineSalesGrantResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.event.OfflineGrantIssuedEvent;
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
  private final OfflineGrantReaderPort grantReaderPort;
  private final QueryBus queryBus;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public IssueOfflineSalesGrantResult handle(IssueOfflineSalesGrantCommand command) {
    var posContext = queryBus.ask(new ResolvePosOperationContextQuery(
        command.tenantId(),
        command.sellerUserId(),
        command.operationalContext(),
        PosOperationAction.REQUEST_OFFLINE_GRANT));

    if (grantReaderPort.existsForFrame(
        command.tenantId(),
        command.sellerUserId(),
        posContext.terminalId(),
        posContext.salesSessionId(),
        OfflineSalesGrantStatus.ACTIVE)) {
      throw ProblemRest.conflict("offline_grant.active_exists");
    }

    var issuedAt = Instant.now();
    var grant = new OfflineSalesGrant(
        OfflineSalesGrantId.of(UUID.randomUUID()),
        command.tenantId(),
        posContext.terminalId(),
        posContext.outletId(),
        posContext.salesSessionId(),
        command.sellerUserId(),
        command.codeBatchId(),
        OfflineSalesGrantStatus.ACTIVE,
        issuedAt,
        command.expiresAt());
    var id = grantWriterPort.save(grant);

    AfterCommit.run(() -> events.publish(new OfflineGrantIssuedEvent(
        EventId.of(idGenerator.newUuid()),
        issuedAt,
        grant.tenantId(),
        id,
        grant.terminalId(),
        grant.outletId(),
        grant.salesSessionId(),
        grant.sellerUserId())));

    return new IssueOfflineSalesGrantResult(id, OfflineSalesGrantStatus.ACTIVE);
  }
}
