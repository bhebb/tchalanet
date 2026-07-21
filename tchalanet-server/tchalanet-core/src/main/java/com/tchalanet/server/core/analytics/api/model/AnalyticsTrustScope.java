package com.tchalanet.server.core.analytics.api.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Identifies the exact business scope whose analytics reliability is being requested.
 *
 * <p>Draw scopes are intentionally a single business date because a draw belongs to one scheduled
 * occurrence. Tenant and seller-terminal scopes may cover a date range.
 */
public record AnalyticsTrustScope(
    AnalyticsTrustScopeType type,
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    DrawId drawId,
    LocalDate from,
    LocalDate to) {

  public AnalyticsTrustScope {
    Objects.requireNonNull(type, "type is required");
    Objects.requireNonNull(from, "from is required");
    Objects.requireNonNull(to, "to is required");
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("from must be on or before to");
    }
    switch (type) {
      case PLATFORM -> requireAbsent(tenantId, sellerTerminalId, drawId);
      case TENANT -> {
        requirePresent(tenantId, "tenantId");
        requireAbsent(sellerTerminalId, drawId);
      }
      case SELLER_TERMINAL -> {
        requirePresent(tenantId, "tenantId");
        requirePresent(sellerTerminalId, "sellerTerminalId");
        requireAbsent(drawId);
      }
      case DRAW -> {
        requirePresent(tenantId, "tenantId");
        requirePresent(drawId, "drawId");
        requireAbsent(sellerTerminalId);
        requireSingleDate(from, to);
      }
      case SELLER_TERMINAL_DRAW -> {
        requirePresent(tenantId, "tenantId");
        requirePresent(sellerTerminalId, "sellerTerminalId");
        requirePresent(drawId, "drawId");
        requireSingleDate(from, to);
      }
    }
  }

  public static AnalyticsTrustScope platform(LocalDate from, LocalDate to) {
    return new AnalyticsTrustScope(AnalyticsTrustScopeType.PLATFORM, null, null, null, from, to);
  }

  public static AnalyticsTrustScope tenant(TenantId tenantId, LocalDate from, LocalDate to) {
    return new AnalyticsTrustScope(AnalyticsTrustScopeType.TENANT, tenantId, null, null, from, to);
  }

  public static AnalyticsTrustScope sellerTerminal(
      TenantId tenantId, SellerTerminalId sellerTerminalId, LocalDate from, LocalDate to) {
    return new AnalyticsTrustScope(
        AnalyticsTrustScopeType.SELLER_TERMINAL, tenantId, sellerTerminalId, null, from, to);
  }

  public static AnalyticsTrustScope draw(TenantId tenantId, DrawId drawId, LocalDate refDate) {
    return new AnalyticsTrustScope(
        AnalyticsTrustScopeType.DRAW, tenantId, null, drawId, refDate, refDate);
  }

  public static AnalyticsTrustScope sellerTerminalDraw(
      TenantId tenantId, SellerTerminalId sellerTerminalId, DrawId drawId, LocalDate refDate) {
    return new AnalyticsTrustScope(
        AnalyticsTrustScopeType.SELLER_TERMINAL_DRAW,
        tenantId,
        sellerTerminalId,
        drawId,
        refDate,
        refDate);
  }

  private static void requirePresent(Object value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " is required for analytics trust scope");
    }
  }

  private static void requireAbsent(Object... values) {
    for (Object value : values) {
      if (value != null) {
        throw new IllegalArgumentException("identifier is not applicable to analytics trust scope");
      }
    }
  }

  private static void requireSingleDate(LocalDate from, LocalDate to) {
    if (!from.equals(to)) {
      throw new IllegalArgumentException("draw trust scope must target one business date");
    }
  }
}
