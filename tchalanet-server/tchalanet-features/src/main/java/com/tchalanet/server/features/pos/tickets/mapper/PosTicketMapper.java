package com.tchalanet.server.features.pos.tickets.mapper;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultProjection;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintCharge;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.features.pos.tickets.model.PosTicketDetailsResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketPageResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PosTicketMapper {

  public PosTicketPageResponse toPageResponse(TicketRow row) {
    return new PosTicketPageResponse(
        row.id(),
        row.ticketCode(),
        row.publicCode(),
        row.status(),
        row.resultStatus(),
        row.settlementStatus(),
        row.drawId(),
        row.sellerTerminalId(),
        row.drawChannelCode(),
        row.resultSlotKey(),
        row.resultProvider(),
        row.resultTimezone(),
        row.drawChannelName(),
        row.drawScheduledAt(),
        row.totalAmountCents(),
        row.winningAmountCents(),
        row.paidAmountCents(),
        row.currency(),
        row.placedAt());
  }

  /**
   * Enriched detail response built from the print view — includes bet lines, charges, draw channel
   * name, seller context, and promotion annotations.
   */
  public PosTicketDetailsResponse toDetailsResponse(TicketPrintView view) {
    return toDetailsResponse(view, null);
  }

  public PosTicketDetailsResponse toDetailsResponse(
      TicketPrintView view, DrawResultProjection drawResult) {
    var identity = view.identity();
    var draw = view.draw();
    var ctx = view.context();
    var money = view.money();
    var meta = view.metadata();
    var lifecycle = view.lifecycle();

    return new PosTicketDetailsResponse(
        identity.ticketId(),
        identity.ticketCode(),
        identity.publicCode(),
        lifecycle.saleStatus(),
        lifecycle.resultStatus(),
        lifecycle.settlementStatus(),
        meta.placedAt(),
        null, // cancelledAt not in TicketPrintView; status=CANCELLED signals it
        draw.drawId(),
        draw.channelCode(),
        draw.resultSlotKey(),
        draw.provider(),
        draw.timezone(),
        draw.drawChannelName(),
        draw.scheduledAt(),
        drawResultNumbers(drawResult),
        ctx != null ? ctx.sellerTerminalId() : null,
        ctx != null ? ctx.sellerTerminalLabel() : null,
        ctx != null ? ctx.sellerTerminalCode() : null,
        ctx != null ? ctx.sellerTerminalDisplayName() : null,
        toLines(view.lines()),
        toCents(money.stake()),
        toCents(money.totalAmount()),
        winningAmountCents(view.lines()),
        meta.currency(),
        toCharges(money.charges()));
  }

  private List<PosTicketDetailsResponse.CashierTicketLineDetailResponse> toLines(
      List<TicketPrintLine> lines) {
    if (lines == null) return List.of();
    return lines.stream()
        .map(
            l ->
                new PosTicketDetailsResponse.CashierTicketLineDetailResponse(
                    l.lineNo(),
                    l.gameCode().name(),
                    l.gameLabel(),
                    l.betType().name(),
                    null, // betTypeLabel — not in TicketPrintLine
                    l.selectionCanonical(),
                    toCents(l.stake()),
                    l.promotional(),
                    l.promotionLabel(),
                    l.resultStatus(),
                    toCents(l.payoutAmount()),
                    toPricingTerms(l),
                    l.appliedSettlementSnapshot()))
        .toList();
  }

  private List<PosTicketDetailsResponse.PricingTermDetailResponse> toPricingTerms(
      TicketPrintLine line) {
    if (line.settlementTermsSnapshot() == null || line.settlementTermsSnapshot().terms() == null) {
      return List.of();
    }
    return line.settlementTermsSnapshot().terms().stream().map(this::toPricingTerm).toList();
  }

  private PosTicketDetailsResponse.PricingTermDetailResponse toPricingTerm(
      SettlementTermSnapshot term) {
    var rule = term.payoutRule();
    return new PosTicketDetailsResponse.PricingTermDetailResponse(
        term.ruleCode(),
        term.sourceBetOption(),
        term.commercialLabel(),
        rule.type(),
        rule.multiplier(),
        rule.fixedAmount(),
        term.source());
  }

  private List<PosTicketDetailsResponse.CashierTicketChargeResponse> toCharges(
      List<TicketPrintCharge> charges) {
    if (charges == null) return List.of();
    return charges.stream()
        .map(
            c ->
                new PosTicketDetailsResponse.CashierTicketChargeResponse(
                    c.type() != null ? c.type().name() : null,
                    c.label(),
                    toCents(c.amount()),
                    c.isWaived(),
                    c.waivedLabel()))
        .toList();
  }

  private long toCents(Money money) {
    if (money == null || money.amount() == null) return 0L;
    return money.amount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
  }

  private long winningAmountCents(List<TicketPrintLine> lines) {
    if (lines == null) return 0L;
    return lines.stream().mapToLong(line -> toCents(line.payoutAmount())).sum();
  }

  private List<String> drawResultNumbers(DrawResultProjection drawResult) {
    if (drawResult == null) return List.of();
    return java.util.stream.Stream.of(
            drawResult.lot1(), drawResult.lot2(), drawResult.lot3(), drawResult.lot4())
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .toList();
  }
}
