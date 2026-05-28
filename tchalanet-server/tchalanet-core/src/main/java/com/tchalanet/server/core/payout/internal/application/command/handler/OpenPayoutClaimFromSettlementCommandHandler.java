package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.api.command.OpenPayoutClaimFromSettlementCommand;
import com.tchalanet.server.core.payout.api.command.OpenPayoutClaimResult;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.internal.domain.event.PayoutClaimOpenedEvent;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaim;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimSource;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenPayoutClaimFromSettlementCommandHandler
    implements CommandHandler<OpenPayoutClaimFromSettlementCommand, OpenPayoutClaimResult> {

    private final PayoutReaderPort reader;
    private final PayoutWriterPort writer;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public OpenPayoutClaimResult handle(OpenPayoutClaimFromSettlementCommand command) {
        var existing = reader.findByTicketId(command.ticketId());
        if (existing.isPresent()) {
            var claim = existing.get();
            log.info(
                "payout.open-claim.idempotent ticketId={} payoutId={} status={}",
                command.ticketId(), claim.id(), claim.status());
            return new OpenPayoutClaimResult(claim.id(), claim.status(), true);
        }

        var now = Instant.now(clock);
        var claim = new PayoutClaim(
            null,
            command.tenantId(),
            command.ticketId(),
            command.drawId(),
            command.amountCents(),
            command.currency(),
            PayoutClaimStatus.OPEN,
            PayoutClaimSource.SALES_SETTLEMENT,
            command.sourceEventId(),
            command.sellingOutletId(),
            command.sellingSessionId(),
            now,
            null, null, null,
            null, null,
            null, null, null,
            null, null, null,
            null, null, null);

        var saved = writer.save(claim);

        var event = new PayoutClaimOpenedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            saved.tenantId(),
            saved.id(),
            saved.ticketId(),
            saved.drawId(),
            saved.amountCents(),
            saved.currency(),
            saved.source(),
            saved.sellingOutletId(),
            saved.sellingSessionId());

        AfterCommit.run(() -> events.publish(event));

        log.info(
            "payout.open-claim.created ticketId={} payoutId={} amountCents={}",
            saved.ticketId(), saved.id(), saved.amountCents());

        return new OpenPayoutClaimResult(saved.id(), saved.status(), false);
    }
}
