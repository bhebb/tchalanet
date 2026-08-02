package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.settlement.AppliedSettlementSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PosTicketDetailsResponse(
    // ── Identity ──────────────────────────────────────────────────────────
    TicketId id,
    String ticketCode,
    String publicCode,
    TicketSaleStatus status,
    Instant placedAt,
    Instant cancelledAt,

    // ── Draw ──────────────────────────────────────────────────────────────
    DrawId drawId,
    String drawChannelCode,
    String resultSlotKey,
    String resultProvider,
    String resultTimezone,
    String drawChannelName, // e.g. "Haïti • Texas • 12:27"
    Instant drawScheduledAt,

    // ── Seller context ────────────────────────────────────────────────────
    SellerTerminalId sellerTerminalId,
    String outletName,
    String terminalCode,
    String sellerDisplayName,

    // ── Bet lines ─────────────────────────────────────────────────────────
    List<CashierTicketLineDetailResponse> lines,

    // ── Money ─────────────────────────────────────────────────────────────
    long stakeCents,
    long totalAmountCents,
    String currency,
    List<CashierTicketChargeResponse> charges) {

  /** A single bet line on the ticket. */
  public record CashierTicketLineDetailResponse(
      int lineNumber,
      String gameCode,
      String gameLabel,
      String betType,
      String betTypeLabel,
      String selection, // display_selection
      long stakeAmountCents,
      boolean promotional, // free game or odds-boosted by a promo
      String promotionLabel, // e.g. "Maryaj gratuit", null if no promo
      TicketLineResultStatus resultStatus,
      long payoutAmountCents,
      List<PricingTermDetailResponse> pricingTerms,
      AppliedSettlementSnapshot appliedSettlement) {}

  /** Immutable payout terms captured on the ticket line at sale time. */
  public record PricingTermDetailResponse(
      SettlementRuleCode ruleCode,
      Short betOption,
      String commercialLabel,
      PayoutRuleType payoutRuleType,
      BigDecimal multiplier,
      BigDecimal fixedAmount,
      SettlementTermSource source) {}

  /** A surcharge applied to the ticket (SMS fee, WhatsApp fee, etc.). */
  public record CashierTicketChargeResponse(
      String type, // e.g. "SMS", "WHATSAPP"
      String label, // i18n label from receipt bundle
      long amountCents,
      boolean waived, // true if a promotion waived this charge
      String waivedLabel // e.g. "Promotion gratuite", null if not waived
      ) {}
}
