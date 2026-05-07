package com.tchalanet.server.core.outlet.domain.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.time.LocalTime;

public record Outlet(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    boolean dayClosed,
    boolean salesBlocked,
    String salesBlockReason,
    Instant salesBlockedAt,
    String timezone,
    LocalTime businessDayCutoff,
    boolean receiptPrintingEnabled,
    String receiptHeaderMessage,
    String receiptFooterMessage,
    boolean requireOpeningFloat,
    boolean autoOpenSession,
    boolean autoCloseSession,
    UserId autoSessionUserId,
    TerminalId autoSessionTerminalId,
    Long defaultOpeningFloatCents,
    AddressId addressId) {

  public static Outlet createNew(TenantId tenantId, String name, String slug, OutletId newId) {
    return new Outlet(
        newId,
        tenantId,
        name,
        slug,
        false,
        false,
        null,
        null,
        "America/Port-au-Prince",
        null,
        true,
        null,
        null,
        true,
        false,
        false,
        null,
        null,
        null,
        null);
  }

  public Outlet closeDay() {
    if (this.dayClosed) return this;
    return new Outlet(
        id,
        tenantId,
        name,
        slug,
        true,
        salesBlocked,
        salesBlockReason,
        salesBlockedAt,
        timezone,
        businessDayCutoff,
        receiptPrintingEnabled,
        receiptHeaderMessage,
        receiptFooterMessage,
        requireOpeningFloat,
        autoOpenSession,
        autoCloseSession,
        autoSessionUserId,
        autoSessionTerminalId,
        defaultOpeningFloatCents,
        addressId);
  }

  public Outlet reopenDay() {
    if (!this.dayClosed) return this;
    return new Outlet(
        id,
        tenantId,
        name,
        slug,
        false,
        salesBlocked,
        salesBlockReason,
        salesBlockedAt,
        timezone,
        businessDayCutoff,
        receiptPrintingEnabled,
        receiptHeaderMessage,
        receiptFooterMessage,
        requireOpeningFloat,
        autoOpenSession,
        autoCloseSession,
        autoSessionUserId,
        autoSessionTerminalId,
        defaultOpeningFloatCents,
        addressId);
  }

  public Outlet blockSales(String reason, Instant when) {
    return new Outlet(
        id,
        tenantId,
        name,
        slug,
        dayClosed,
        true,
        reason,
        when,
        timezone,
        businessDayCutoff,
        receiptPrintingEnabled,
        receiptHeaderMessage,
        receiptFooterMessage,
        requireOpeningFloat,
        autoOpenSession,
        autoCloseSession,
        autoSessionUserId,
        autoSessionTerminalId,
        defaultOpeningFloatCents,
        addressId);
  }

  public Outlet unblockSales() {
    if (!this.salesBlocked) return this;
    return new Outlet(
        id,
        tenantId,
        name,
        slug,
        dayClosed,
        false,
        null,
        null,
        timezone,
        businessDayCutoff,
        receiptPrintingEnabled,
        receiptHeaderMessage,
        receiptFooterMessage,
        requireOpeningFloat,
        autoOpenSession,
        autoCloseSession,
        autoSessionUserId,
        autoSessionTerminalId,
        defaultOpeningFloatCents,
        addressId);
  }

  /** Pure-domain decision: can this outlet sell now? Time/clock concerns belong to caller. */
  public SalesCapability salesCapability() {
    if (dayClosed) {
      return SalesCapability.blocked("DAY_CLOSED");
    }
    if (salesBlocked) {
      return SalesCapability.blocked(
          salesBlockReason != null ? salesBlockReason : "SALES_BLOCKED");
    }
    return SalesCapability.allowedCapability();
  }

  public boolean canSell() {
    return !dayClosed && !salesBlocked;
  }

  public Outlet applyConfigPatch(
      Boolean salesBlockedPatch,
      String salesBlockReasonPatch,
      String timezonePatch,
      LocalTime businessDayCutoffPatch,
      Boolean receiptPrintingEnabledPatch,
      String receiptHeaderMessagePatch,
      String receiptFooterMessagePatch,
      Boolean requireOpeningFloatPatch,
      Boolean autoOpenSessionPatch,
      Boolean autoCloseSessionPatch,
      UserId autoSessionUserIdPatch,
      TerminalId autoSessionTerminalIdPatch,
      Long defaultOpeningFloatCentsPatch,
      Instant when) {
    boolean newSalesBlocked = salesBlockedPatch == null ? this.salesBlocked : salesBlockedPatch;
    String newSalesBlockReason =
        salesBlockReasonPatch == null ? this.salesBlockReason : salesBlockReasonPatch;
    Instant newSalesBlockedAt =
        newSalesBlocked ? (when == null ? this.salesBlockedAt : when) : null;
    return new Outlet(
        id,
        tenantId,
        name,
        slug,
        dayClosed,
        newSalesBlocked,
        newSalesBlockReason,
        newSalesBlockedAt,
        timezonePatch == null ? this.timezone : timezonePatch,
        businessDayCutoffPatch == null ? this.businessDayCutoff : businessDayCutoffPatch,
        receiptPrintingEnabledPatch == null
            ? this.receiptPrintingEnabled
            : receiptPrintingEnabledPatch,
        receiptHeaderMessagePatch == null ? this.receiptHeaderMessage : receiptHeaderMessagePatch,
        receiptFooterMessagePatch == null ? this.receiptFooterMessage : receiptFooterMessagePatch,
        requireOpeningFloatPatch == null ? this.requireOpeningFloat : requireOpeningFloatPatch,
        autoOpenSessionPatch == null ? this.autoOpenSession : autoOpenSessionPatch,
        autoCloseSessionPatch == null ? this.autoCloseSession : autoCloseSessionPatch,
        autoSessionUserIdPatch == null ? this.autoSessionUserId : autoSessionUserIdPatch,
        autoSessionTerminalIdPatch == null
            ? this.autoSessionTerminalId
            : autoSessionTerminalIdPatch,
        defaultOpeningFloatCentsPatch == null
            ? this.defaultOpeningFloatCents
            : defaultOpeningFloatCentsPatch,
        addressId);
  }

  public Outlet withAddressId(AddressId newAddressId) {
    return new Outlet(
        id,
        tenantId,
        name,
        slug,
        dayClosed,
        salesBlocked,
        salesBlockReason,
        salesBlockedAt,
        timezone,
        businessDayCutoff,
        receiptPrintingEnabled,
        receiptHeaderMessage,
        receiptFooterMessage,
        requireOpeningFloat,
        autoOpenSession,
        autoCloseSession,
        autoSessionUserId,
        autoSessionTerminalId,
        defaultOpeningFloatCents,
        newAddressId);
  }
}
