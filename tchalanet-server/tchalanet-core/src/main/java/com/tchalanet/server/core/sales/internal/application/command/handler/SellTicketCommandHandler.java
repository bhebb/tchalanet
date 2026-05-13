package com.tchalanet.server.core.sales.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.sales.api.command.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.SellTicketResult;
import com.tchalanet.server.core.sales.internal.application.factory.TicketSaleFactory;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.TicketSalePolicyService;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.TicketLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SellTicketCommandHandler implements CommandHandler<SellTicketCommand, SellTicketResult> {

    private final TicketSalePolicyService salePolicy;
    private final TicketSaleFactory ticketFactory;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {
        var prepared = salePolicy.prepareSale(command);
        var session = prepared.session();
        var now = prepared.now();

        if (session == null) {
            throw new IllegalStateException("Open sales session is required to sell a ticket");
        }

        enforceSingleGameCode(prepared.ticketLines());

        if (prepared.limits().outcome() == BreachOutcome.BLOCK) {
            var approvalRequestId = ApprovalRequestId.of(idGenerator.newUuid());

            var pending =
                ticketFactory.newPendingApprovalTicket(
                    command.tenantId(),
                    command.terminalId(),
                    session,
                    prepared.draw(),
                    prepared.ticketLines(),
                    approvalRequestId,
                    now);

            var saved = ticketWriter.save(pending);

            ApiResponseContext.get()
                .addNotice(
                    new ApiNotice(
                        "APPROVAL_REQUIRED",
                        "Transaction requires approval",
                        "sales",
                        NoticeSeverity.WARN,
                        Map.of("approvalRequestId", approvalRequestId.value())));

            return new SellTicketResult(saved, SellTicketOutcome.PENDING_APPROVAL, approvalRequestId);
        }

        var sold =
            ticketFactory.newSoldTicket(
                command.tenantId(),
                command.terminalId(),
                session,
                prepared.draw(),
                prepared.ticketLines(),
                Currency.getInstance(command.currency()),
                now);

        var saved = ticketWriter.save(sold);

        // TODO(sales-refactor): restore TicketPlacedEvent payload once sales aggregate mapping is stabilized.
        AfterCommit.run(() -> {
            // Intentionally no-op until sales event contract is finalized.
        });

        var outcome =
            prepared.limits().outcome() == BreachOutcome.WARN
                ? SellTicketOutcome.SUCCESS_WITH_WARNINGS
                : SellTicketOutcome.SUCCESS;

        log.info("Ticket sold ticketId={} status={}", saved.getId(), saved.getSaleStatus());

        return new SellTicketResult(saved, outcome, null);
    }

    private void enforceSingleGameCode(List<TicketLine> lines) {
        var primaryGame = lines.stream().map(TicketLine::gameCode).findFirst().orElseThrow();

        var mixedGameCodes = lines.stream().anyMatch(line -> !primaryGame.equals(line.gameCode()));

        if (mixedGameCodes) {
            throw new IllegalStateException("Mixed game_code per ticket is not supported yet (MVP)");
        }
    }


    private static long toCentsExact(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }

        return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }
}
