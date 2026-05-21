package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.query.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.api.query.preview.PreviewTicketSaleQuery;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierTicketsService {

    private final QueryBus queryBus;
    private final CommandBus commandBus;
    private final SellerOperationalContextResolver sellerContextResolver;
    private final CashierTicketMapper mapper;

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
            SaleCommunicationOptions.none()
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
        var view = queryBus.ask(new GetTicketDetailsQuery(ticketId));
        return mapper.toDetailsResponse(view);
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
