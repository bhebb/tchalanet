package com.tchalanet.server.core.outlet.internal.domain.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.time.LocalTime;

public record Outlet(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    OutletKind kind,
    String partnerRef,
    SalesZoneId zoneId,
    OutletStatus status,
    boolean dayClosed,
    BlockState outletBlock,
    BlockState salesBlock,
    BlockState payoutBlock,
    BlockState offlineSalesBlock,
    String timezone,
    boolean receiptPrintingEnabled,
    String receiptHeaderMessage,
    String receiptFooterMessage,
    boolean requireOpeningFloat,
    boolean autoSessionOpenEnabled,
    boolean autoSessionCloseEnabled,
    LocalTime sessionOpenTime,
    LocalTime sessionCloseTime,
    Long defaultOpeningFloatCents,
    AddressId addressId) {

  public static Outlet createNew(
      TenantId tenantId,
      String name,
      String slug,
      OutletId newId,
      OutletKind kind,
      String partnerRef,
      SalesZoneId zoneId) {
    return new Outlet(
        newId,
        tenantId,
        name,
        slug,
        kind != null ? kind : OutletKind.OWNED_SHOP,
        partnerRef,
        zoneId,
        OutletStatus.DRAFT,
        false,
        BlockState.none(),
        BlockState.none(),
        BlockState.none(),
        BlockState.none(),
        "America/Port-au-Prince",
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

  // ── Day ──────────────────────────────────────────────────────────────

  public Outlet closeDay() {
    if (dayClosed) return this;
    return withDayClosed(true);
  }

  public Outlet reopenDay() {
    if (!dayClosed) return this;
    return withDayClosed(false);
  }

  // ── Outlet-level block (overrides everything) ─────────────────────────

  public Outlet blockOutlet(String reason, Instant at, UserId by) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock.block(reason, at, by), salesBlock, payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  public Outlet unblockOutlet() {
    if (!outletBlock.blocked()) return this;
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock.unblock(), salesBlock, payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  // ── Sales block ──────────────────────────────────────────────────────

  public Outlet blockSales(String reason, Instant at, UserId by) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock.block(reason, at, by), payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  public Outlet unblockSales() {
    if (!salesBlock.blocked()) return this;
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock.unblock(), payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  // ── Payout block ─────────────────────────────────────────────────────

  public Outlet blockPayout(String reason, Instant at, UserId by) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock, payoutBlock.block(reason, at, by), offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  public Outlet unblockPayout() {
    if (!payoutBlock.blocked()) return this;
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock, payoutBlock.unblock(), offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  // ── Offline sales block ──────────────────────────────────────────────

  public Outlet blockOfflineSales(String reason, Instant at, UserId by) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock, payoutBlock, offlineSalesBlock.block(reason, at, by),
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  public Outlet unblockOfflineSales() {
    if (!offlineSalesBlock.blocked()) return this;
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock, payoutBlock, offlineSalesBlock.unblock(),
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  // ── Capability checks ────────────────────────────────────────────────

  public SalesCapability salesCapability() {
    if (status != OutletStatus.ACTIVE) {
      return SalesCapability.blocked("OUTLET_NOT_ACTIVE");
    }
    if (outletBlock.blocked()) {
      return SalesCapability.blocked(
          outletBlock.reason() != null ? outletBlock.reason() : "OUTLET_BLOCKED");
    }
    if (dayClosed) {
      return SalesCapability.blocked("DAY_CLOSED");
    }
    if (salesBlock.blocked()) {
      return SalesCapability.blocked(
          salesBlock.reason() != null ? salesBlock.reason() : "SALES_BLOCKED");
    }
    return SalesCapability.allowedCapability();
  }

  public boolean canSell() {
    return status == OutletStatus.ACTIVE
        && !outletBlock.blocked()
        && !dayClosed
        && !salesBlock.blocked();
  }

  public boolean canPayout() {
    return status == OutletStatus.ACTIVE
        && !outletBlock.blocked()
        && !payoutBlock.blocked();
  }

  public boolean canAcceptOfflineSales() {
    return status == OutletStatus.ACTIVE
        && !outletBlock.blocked()
        && !salesBlock.blocked()
        && !offlineSalesBlock.blocked();
  }

  // ── Config patch ─────────────────────────────────────────────────────

  public Outlet applyConfigPatch(
      String partnerRefPatch,
      SalesZoneId zoneIdPatch,
      String timezonePatch,
      Boolean receiptPrintingEnabledPatch,
      String receiptHeaderMessagePatch,
      String receiptFooterMessagePatch,
      Boolean requireOpeningFloatPatch,
      Boolean autoSessionOpenEnabledPatch,
      Boolean autoSessionCloseEnabledPatch,
      LocalTime sessionOpenTimePatch,
      LocalTime sessionCloseTimePatch,
      Long defaultOpeningFloatCentsPatch) {
    return new Outlet(
        id, tenantId, name, slug, kind,
        partnerRefPatch != null ? partnerRefPatch : partnerRef,
        zoneIdPatch != null ? zoneIdPatch : zoneId,
        status, dayClosed, outletBlock, salesBlock, payoutBlock, offlineSalesBlock,
        timezonePatch != null ? timezonePatch : timezone,
        receiptPrintingEnabledPatch != null ? receiptPrintingEnabledPatch : receiptPrintingEnabled,
        receiptHeaderMessagePatch != null ? receiptHeaderMessagePatch : receiptHeaderMessage,
        receiptFooterMessagePatch != null ? receiptFooterMessagePatch : receiptFooterMessage,
        requireOpeningFloatPatch != null ? requireOpeningFloatPatch : requireOpeningFloat,
        autoSessionOpenEnabledPatch != null ? autoSessionOpenEnabledPatch : autoSessionOpenEnabled,
        autoSessionCloseEnabledPatch != null ? autoSessionCloseEnabledPatch : autoSessionCloseEnabled,
        sessionOpenTimePatch != null ? sessionOpenTimePatch : sessionOpenTime,
        sessionCloseTimePatch != null ? sessionCloseTimePatch : sessionCloseTime,
        defaultOpeningFloatCentsPatch != null ? defaultOpeningFloatCentsPatch : defaultOpeningFloatCents,
        addressId);
  }

  // ── Status ───────────────────────────────────────────────────────────

  public Outlet withStatus(OutletStatus newStatus) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, newStatus,
        dayClosed, outletBlock, salesBlock, payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }

  public Outlet withAddressId(AddressId newAddressId) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        dayClosed, outletBlock, salesBlock, payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, newAddressId);
  }

  private Outlet withDayClosed(boolean newDayClosed) {
    return new Outlet(id, tenantId, name, slug, kind, partnerRef, zoneId, status,
        newDayClosed, outletBlock, salesBlock, payoutBlock, offlineSalesBlock,
        timezone, receiptPrintingEnabled, receiptHeaderMessage, receiptFooterMessage,
        requireOpeningFloat, autoSessionOpenEnabled, autoSessionCloseEnabled,
        sessionOpenTime, sessionCloseTime, defaultOpeningFloatCents, addressId);
  }
}
