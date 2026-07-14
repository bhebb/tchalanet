package com.tchalanet.server.features.tenantadmin.tickets.model;

import java.util.List;

/**
 * Admin/support view of a ticket's computed settlement variants (per line).
 *
 * <p>The variant is recomputed on demand from {@code betType + betOption + selection}; it is not
 * persisted. Technical variants (e.g. {@code Permuté · 24-way}) are exposed here for admin/support
 * only — never on the seller terminal or the customer receipt.
 */
public record AdminTicketSettlementResponse(String ticketId, List<LineSettlementView> lines) {

  public record LineSettlementView(
      int lineNo,
      String gameCode,
      String betType,
      Short betOption,
      String selection,
      String commercialLabel,
      String variant,
      String adminLabel,
      boolean resolved,
      String error) {}
}
