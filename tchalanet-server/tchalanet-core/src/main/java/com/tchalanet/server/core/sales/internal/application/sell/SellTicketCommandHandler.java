package com.tchalanet.server.core.sales.internal.application.sell;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateSaleAutonomyQuery;
import com.tchalanet.server.core.sales.api.command.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.SellTicketResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketCodeGeneratorPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.TicketCreationService;
import com.tchalanet.server.core.sales.internal.application.service.TicketLinePreparationService;
import com.tchalanet.server.core.sales.internal.application.service.TicketMoneyCalculator;
import com.tchalanet.server.core.sales.internal.application.validation.PosOperationActorContext;
import com.tchalanet.server.core.sales.internal.application.validation.PosSaleOperationValidator;
import com.tchalanet.server.core.session.api.query.GetOpenSalesSessionForTerminalQuery;
import com.tchalanet.server.core.terminal.api.query.GetTerminalSalesContextQuery;
import lombok.RequiredArgsConstructor;

/**
 * Concurrency contract (v1 — no external cache):
 * 1. PosSaleOperationValidator runs pre-transaction (validates terminal/outlet/session snapshot).
 * 2. Inside {@code @TchTx}: terminal.locked() is re-checked before persisting the ticket.
 *    If the terminal was locked between pre-check and commit, the handler returns TERMINAL_LOCKED.
 * 3. Race on session close: the DB unique partial index on (tenant_id, terminal_id, status='OPEN')
 *    prevents a second ticket from landing on a session that was concurrently closed.
 */
@UseCase
@RequiredArgsConstructor
public class SellTicketCommandHandler implements CommandHandler<SellTicketCommand, SellTicketResult> {

    private final QueryBus queryBus;
    private final TicketMoneyCalculator moneyCalculator;
    private final TicketLinePreparationService linePreparationService;
    private final TicketCreationService ticketCreationService;
    private final TicketCodeGeneratorPort codeGenerator;
    private final TicketWriterPort ticketWriter;
    private final PosSaleOperationValidator posSaleOperationValidator;

    @Override
    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {

        var actor = new PosOperationActorContext(
            command.tenantId(),
            command.sellerUserId(),
            command.terminalId(),
            command.outletId(),
            command.salesSessionId()
        );

        var validated = posSaleOperationValidator.validate(
            actor,
            cmd.operationalContext()
        );

        var terminal = queryBus.ask(new GetTerminalSalesContextQuery(
            command.terminalId(),
            command.sellerUserId()));

        if (terminal.locked()) {
            return SellTicketResult.rejected("TERMINAL_LOCKED");
        }

        var session = queryBus.ask(new GetOpenSalesSessionForTerminalQuery(
            command.terminalId(),
            command.sellerUserId()));

        var money = moneyCalculator.calculate(command.lines(), command.feeAmount());
        var lines = linePreparationService.prepare(command.lines());

        var limit = queryBus.ask(new EvaluateSaleLimitPolicyQuery(
            terminal.outletId(),
            command.terminalId(),
            command.sellerUserId(),
            money.stakeAmount(),
            money.totalAmount()));

        if (limit.blocked()) {
            return SellTicketResult.rejected("LIMIT_POLICY_BLOCKED:" + limit.reason());
        }

        var autonomy = queryBus.ask(new EvaluateSaleAutonomyQuery(
            terminal.outletId(),
            command.terminalId(),
            command.sellerUserId(),
            money.totalAmount()));

        if (limit.approvalRequired() || autonomy.approvalRequired()) {
            return SellTicketResult.rejected("APPROVAL_REQUIRED_NOT_IMPLEMENTED_IN_THIS_PATCH");
        }

        // TODO: create Ticket with existing draw model, save, publish TicketPlacedEvent after commit.
        return SellTicketResult.rejected("TODO_CREATE_TICKET_AFTER_WIRING_DRAW_AND_EXISTING_MODELS");
    }
}
