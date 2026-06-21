package com.tchalanet.server.features.pos.tickets.mapper;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintCharge;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.features.pos.tickets.model.PosTicketDetailsResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketPageResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PosTicketMapper {

    public PosTicketPageResponse toPageResponse(TicketRow row) {
        return new PosTicketPageResponse(
            row.id(),
            row.ticketCode(),
            row.publicCode(),
            row.status(),
            row.drawId(),
            row.drawChannelName(),
            row.drawScheduledAt(),
            row.totalAmountCents(),
            row.currency(),
            row.placedAt());
    }

    /**
     * Enriched detail response built from the print view — includes bet lines,
     * charges, draw channel name, seller context, and promotion annotations.
     */
    public PosTicketDetailsResponse toDetailsResponse(TicketPrintView view) {
        var identity = view.identity();
        var draw     = view.draw();
        var ctx      = view.context();
        var money    = view.money();
        var meta     = view.metadata();
        var lifecycle = view.lifecycle();

        return new PosTicketDetailsResponse(
            identity.ticketId(),
            identity.ticketCode(),
            identity.publicCode(),
            lifecycle.saleStatus(),
            meta.placedAt(),
            null,           // cancelledAt not in TicketPrintView; status=CANCELLED signals it
            draw.drawId(),
            draw.drawChannelName(),
            draw.scheduledAt(),
            ctx != null ? ctx.sellerTerminalLabel()      : null,
            ctx != null ? ctx.sellerTerminalCode()      : null,
            ctx != null ? ctx.sellerTerminalDisplayName() : null,
            toLines(view.lines()),
            toCents(money.stake()),
            toCents(money.totalAmount()),
            meta.currency(),
            toCents(money.potentialPayoutAmount()),
            toCharges(money.charges())
        );
    }

    private List<PosTicketDetailsResponse.CashierTicketLineDetailResponse> toLines(
            List<TicketPrintLine> lines) {
        if (lines == null) return List.of();
        return lines.stream()
            .map(l -> new PosTicketDetailsResponse.CashierTicketLineDetailResponse(
                l.lineNo(),
                l.gameCode().name(),
                l.gameLabel(),
                l.betType().name(),
                null,                               // betTypeLabel — not in TicketPrintLine
                l.selectionCanonical(),
                toCents(l.stake()),
                toCents(l.potentialPayout()),
                l.promotional(),
                l.promotionLabel()
            ))
            .toList();
    }

    private List<PosTicketDetailsResponse.CashierTicketChargeResponse> toCharges(
            List<TicketPrintCharge> charges) {
        if (charges == null) return List.of();
        return charges.stream()
            .map(c -> new PosTicketDetailsResponse.CashierTicketChargeResponse(
                c.type() != null ? c.type().name() : null,
                c.label(),
                toCents(c.amount()),
                c.isWaived(),
                c.waivedLabel()
            ))
            .toList();
    }

    private long toCents(Money money) {
        if (money == null || money.amount() == null) return 0L;
        return money.amount()
            .multiply(java.math.BigDecimal.valueOf(100))
            .longValue();
    }
}
