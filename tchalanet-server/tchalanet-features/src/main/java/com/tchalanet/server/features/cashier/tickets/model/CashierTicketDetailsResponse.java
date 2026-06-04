package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;

import java.time.Instant;
import java.util.List;

public record CashierTicketDetailsResponse(
    // ── Identity ──────────────────────────────────────────────────────────
    TicketId id,
    String ticketCode,
    String publicCode,
    TicketSaleStatus status,
    Instant placedAt,
    Instant cancelledAt,

    // ── Draw ──────────────────────────────────────────────────────────────
    DrawId drawId,
    String drawChannelName,    // e.g. "Haïti • Texas • 12:27"
    Instant drawScheduledAt,

    // ── Seller context ────────────────────────────────────────────────────
    String outletName,
    String terminalCode,
    String sellerDisplayName,

    // ── Bet lines ─────────────────────────────────────────────────────────
    List<CashierTicketLineDetailResponse> lines,

    // ── Money ─────────────────────────────────────────────────────────────
    long stakeCents,
    long totalAmountCents,
    String currency,
    long potentialPayoutCents,
    List<CashierTicketChargeResponse> charges
) {

    /** A single bet line on the ticket. */
    public record CashierTicketLineDetailResponse(
        int lineNumber,
        String gameCode,
        String gameLabel,
        String betType,
        String betTypeLabel,
        String selection,       // display_selection
        long stakeAmountCents,
        long potentialPayoutCents,
        boolean promotional,    // free game or odds-boosted by a promo
        String promotionLabel   // e.g. "Maryaj gratuit", null if no promo
    ) {}

    /** A surcharge applied to the ticket (SMS fee, WhatsApp fee, etc.). */
    public record CashierTicketChargeResponse(
        String type,            // e.g. "SMS", "WHATSAPP"
        String label,           // i18n label from receipt bundle
        long amountCents,
        boolean waived,         // true if a promotion waived this charge
        String waivedLabel      // e.g. "Promotion gratuite", null if not waived
    ) {}
}
