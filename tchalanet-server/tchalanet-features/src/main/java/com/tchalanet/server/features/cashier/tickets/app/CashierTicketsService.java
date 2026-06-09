package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.api.query.PayoutRow;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import com.tchalanet.server.core.sales.api.model.verification.TicketCashierVerificationView;
import com.tchalanet.server.core.sales.api.query.GetTicketForCashierVerificationQuery;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.api.query.preview.PreviewTicketSaleQuery;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
import com.tchalanet.server.features.cashier.tickets.model.CashierAction;
import com.tchalanet.server.features.cashier.tickets.model.CashierActionType;
import com.tchalanet.server.features.cashier.tickets.mapper.CashierTicketMapper;
import com.tchalanet.server.features.cashier.tickets.model.CashierSellTicketRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierSellTicketResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketBackupView;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketCancelRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketCancelResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketDetailsResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketLineRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPageResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPreviewRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPreviewResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketVerificationResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketVerificationSeverity;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketVerificationStatus;
import com.tchalanet.server.features.cashier.tickets.model.CashierVerifyTicketRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierTicketsService {

    private final QueryBus queryBus;
    private final CommandBus commandBus;
    private final SellerOperationalContextResolver sellerContextResolver;
    private final CashierTicketMapper mapper;
    private final TicketScanResolver ticketScanResolver;
    private final com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort ticketPrintReader;

    public CashierTicketPreviewResponse preview(
        TchRequestContext ctx,
        CashierTicketPreviewRequest request
    ) {
        validateSellerContext(ctx, request.terminalId());
        var result = queryBus.ask(new PreviewTicketSaleQuery(
            DrawId.of(request.drawId()),
            drawChannelId(request.drawChannelId()),
            CurrencyCode.of(request.currency()),
            lines(request.lines())
        ));
        return new CashierTicketPreviewResponse(
            result.decision(),
            result.issues(),
            result.actionAvailability(),
            result.sellerInstruction(),
            result.warning()
        );
    }

    public CashierSellTicketResponse sell(TchRequestContext ctx, CashierSellTicketRequest request) {
        validateSellerContext(ctx, request.terminalId());
        var result = commandBus.execute(new SellTicketCommand(
            DrawId.of(request.drawId()),
            drawChannelId(request.drawChannelId()),
            CurrencyCode.of(request.currency()),
            lines(request.lines()),
            SaleCommunicationOptions.none(),
            request.promotionChoices()
        ));
        return new CashierSellTicketResponse(
            result.outcome(),
            result.ticketId(),
            result.ticketCode(),
            result.publicCode(),
            result.saleStatus(),
            result.issues(),
            CashierTicketBackupView.from(result.backup()),
            result.actionAvailability(),
            result.sellerInstruction()
        );
    }

    public CashierTicketCancelResponse cancel(
        TchRequestContext ctx,
        TicketId ticketId,
        CashierTicketCancelRequest request
    ) {
        validateSellerContext(ctx, request.terminalId());
        var result = commandBus.execute(new CancelTicketCommand(ticketId, request.reason()));
        return new CashierTicketCancelResponse(
            result.ticketId(),
            result.outcome(),
            result.cancelledAt(),
            result.issues()
        );
    }

    public TchPage<CashierTicketPageResponse> listTickets(Pageable pageable) {
        var result = queryBus.ask(new ListTicketsQuery(
            null, null, null, null, null, null, new TchPageRequest(pageable)));
        return TchPageMapper.map(result, mapper::toPageResponse);
    }

    public CashierTicketDetailsResponse getDetails(TicketId ticketId) {
        // Use the print view — it includes publicCode, drawChannelName, seller context,
        // bet lines (with promo flags), and charges. Richer than TicketDetailsView.
        var printView = ticketPrintReader.findPrintViewRequired(ticketId);
        return mapper.toDetailsResponse(printView);
    }

    public CashierTicketVerificationResponse verify(
        TchRequestContext ctx,
        CashierVerifyTicketRequest request
    ) {
        if (ctx.operationalContext() == null || !ctx.operationalContext().trustedForSensitiveOperation()) {
            return response(
                CashierTicketVerificationStatus.OPERATION_NOT_ALLOWED,
                CashierTicketVerificationSeverity.ERROR,
                "pos.ticket.verify.operation_not_allowed.title",
                "pos.ticket.verify.operation_not_allowed.message",
                Map.of(),
                List.of(CashierAction.enabled(CashierActionType.CONTACT_ADMIN,
                    "pos.action.contact_admin", Map.of()))
            );
        }

        var publicCode = ticketScanResolver.resolvePublicCode(request.scannedValue());
        try {
            var ticket = queryBus.ask(new GetTicketForCashierVerificationQuery(publicCode));
            var payout = latestPayout(ticket);
            return verificationResponse(ticket, payout);
        } catch (ProblemRestException ex) {
            return response(
                CashierTicketVerificationStatus.NOT_FOUND,
                CashierTicketVerificationSeverity.ERROR,
                "pos.ticket.verify.not_found.title",
                "pos.ticket.verify.not_found.message",
                Map.of("publicCode", publicCode),
                List.of(CashierAction.enabled(CashierActionType.NONE, "pos.action.none", Map.of()))
            );
        }
    }

    private CashierTicketVerificationResponse verificationResponse(
        TicketCashierVerificationView ticket,
        PayoutRow payout
    ) {
        var baseParams = params(ticket, payout);
        if (ticket.customerStatus() == CustomerTicketStatus.CANCELLED) {
            return status(ticket, payout, CashierTicketVerificationStatus.CANCELLED,
                CashierTicketVerificationSeverity.ERROR, baseParams);
        }
        if (ticket.customerStatus() == CustomerTicketStatus.VOIDED) {
            return status(ticket, payout, CashierTicketVerificationStatus.VOIDED,
                CashierTicketVerificationSeverity.ERROR, baseParams);
        }
        if (ticket.customerStatus() == CustomerTicketStatus.AWAITING_RESULT) {
            var pendingStatus = ticket.drawScheduledAt() != null && ticket.drawScheduledAt().isAfter(Instant.now())
                ? CashierTicketVerificationStatus.NOT_PAYABLE_PENDING_DRAW
                : CashierTicketVerificationStatus.NOT_PAYABLE_RESULT_PENDING;
            return status(ticket, payout, pendingStatus, CashierTicketVerificationSeverity.INFO, baseParams);
        }
        if (ticket.customerStatus() == CustomerTicketStatus.LOST) {
            return status(ticket, payout, CashierTicketVerificationStatus.NOT_PAYABLE_LOST,
                CashierTicketVerificationSeverity.INFO, baseParams);
        }
        if (payout != null && payout.status() == PayoutClaimStatus.PAID) {
            return status(ticket, payout, CashierTicketVerificationStatus.ALREADY_PAID,
                CashierTicketVerificationSeverity.SUCCESS, baseParams);
        }
        if (ticket.customerStatus() == CustomerTicketStatus.WON_PAID) {
            return status(ticket, payout, CashierTicketVerificationStatus.ALREADY_PAID,
                CashierTicketVerificationSeverity.SUCCESS, baseParams);
        }
        if (ticket.customerStatus() == CustomerTicketStatus.CORRECTED) {
            return status(ticket, payout, CashierTicketVerificationStatus.REPAIR_REQUIRED,
                CashierTicketVerificationSeverity.WARNING, baseParams);
        }
        if (ticket.customerStatus() == CustomerTicketStatus.WON_CLAIMABLE) {
            if (payout == null) {
                return status(ticket, null, CashierTicketVerificationStatus.REPAIR_REQUIRED,
                    CashierTicketVerificationSeverity.WARNING, baseParams);
            }
            if (payout.status() == PayoutClaimStatus.OPEN) {
                return status(ticket, payout, CashierTicketVerificationStatus.PAYABLE,
                    CashierTicketVerificationSeverity.SUCCESS, baseParams);
            }
            return status(ticket, payout, CashierTicketVerificationStatus.BLOCKED,
                CashierTicketVerificationSeverity.WARNING, baseParams);
        }
        return status(ticket, payout, CashierTicketVerificationStatus.BLOCKED,
            CashierTicketVerificationSeverity.WARNING, baseParams);
    }

    private CashierTicketVerificationResponse status(
        TicketCashierVerificationView ticket,
        PayoutRow payout,
        CashierTicketVerificationStatus status,
        CashierTicketVerificationSeverity severity,
        Map<String, Object> params
    ) {
        return response(
            status,
            severity,
            "pos.ticket.verify." + key(status) + ".title",
            "pos.ticket.verify." + key(status) + ".message",
            params,
            actions(ticket, payout, status)
        );
    }

    private CashierTicketVerificationResponse response(
        CashierTicketVerificationStatus status,
        CashierTicketVerificationSeverity severity,
        String titleKey,
        String messageKey,
        Map<String, Object> params,
        List<CashierAction> actions
    ) {
        return new CashierTicketVerificationResponse(
            status.name(),
            severity.name(),
            titleKey,
            messageKey,
            params,
            actions
        );
    }

    private List<CashierAction> actions(
        TicketCashierVerificationView ticket,
        PayoutRow payout,
        CashierTicketVerificationStatus status
    ) {
        if (status == CashierTicketVerificationStatus.PAYABLE && payout != null) {
            return List.of(
                CashierAction.enabled(CashierActionType.EXECUTE_PAYOUT,
                    "pos.action.execute_payout",
                    Map.of("payoutId", payout.id().value().toString())),
                CashierAction.enabled(CashierActionType.VIEW_TICKET,
                    "pos.action.view_ticket",
                    Map.of("publicCode", ticket.publicCode()))
            );
        }
        if (status == CashierTicketVerificationStatus.NOT_FOUND) {
            return List.of(CashierAction.enabled(CashierActionType.NONE, "pos.action.none", Map.of()));
        }
        return List.of(
            CashierAction.enabled(CashierActionType.VIEW_TICKET,
                "pos.action.view_ticket",
                Map.of("publicCode", ticket.publicCode())),
            CashierAction.enabled(CashierActionType.REPRINT_TICKET,
                "pos.action.reprint_ticket",
                Map.of("publicCode", ticket.publicCode())),
            CashierAction.enabled(CashierActionType.RESEND_TICKET,
                "pos.action.resend_ticket",
                Map.of("publicCode", ticket.publicCode()))
        );
    }

    private PayoutRow latestPayout(TicketCashierVerificationView ticket) {
        var payouts = queryBus.ask(new ListPayoutsQuery(
            null,
            ticket.ticketId(),
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        ));
        return payouts.items().stream()
            .max(Comparator.comparing(PayoutRow::openedAt))
            .orElse(null);
    }

    private Map<String, Object> params(TicketCashierVerificationView ticket, PayoutRow payout) {
        var params = new LinkedHashMap<String, Object>();
        params.put("publicCode", ticket.publicCode());
        params.put("displayCode", ticket.displayCode());
        params.put("ticketCode", ticket.ticketCode());
        params.put("ticketStatus", ticket.customerStatus().name());
        if (ticket.winningAmount() != null) {
            params.put("amount", ticket.winningAmount().amount().toPlainString());
            params.put("currency", ticket.winningAmount().currency().value());
        }
        if (payout != null) {
            params.put("payoutId", payout.id().value().toString());
            params.put("payoutStatus", payout.status().name());
        }
        return params;
    }

    private String key(CashierTicketVerificationStatus status) {
        return status.name().toLowerCase(java.util.Locale.ROOT);
    }

    private void validateSellerContext(TchRequestContext ctx, java.util.UUID terminalId) {
        sellerContextResolver.resolve(new ResolveSellerOperationalContextRequest(
            ctx,
            TerminalId.of(terminalId),
            SellerOperation.SELL
        ));
    }

    private DrawChannelId drawChannelId(java.util.UUID value) {
        return value == null ? null : DrawChannelId.of(value);
    }

    private List<SellTicketLineInput> lines(List<CashierTicketLineRequest> lines) {
        return java.util.stream.IntStream.range(0, lines.size())
            .mapToObj(index -> line(index, lines.get(index)))
            .toList();
    }

    private SellTicketLineInput line(int index, CashierTicketLineRequest line) {
        return new SellTicketLineInput(
            index + 1,
            line.gameCode(),
            line.betType(),
            line.selection(),
            line.betOption(),
            line.stake()
        );
    }
}
