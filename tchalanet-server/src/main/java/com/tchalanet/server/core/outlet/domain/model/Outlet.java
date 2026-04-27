package com.tchalanet.server.core.outlet.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public final class Outlet {
  private final OutletId id;
  private final TenantId tenantId;
  private final String name;
  private final String slug;
  private final boolean dayClosed;
  private final boolean salesBlocked;
  private final String salesBlockReason;
  private final Instant salesBlockedAt;
  private final String timezone;
  private final LocalTime businessDayCutoff;
  private final boolean receiptPrintingEnabled;
  private final String receiptHeaderMessage;
  private final String receiptFooterMessage;
  private final boolean requireOpeningFloat;
  private final UUID addressId;

  public Outlet(OutletId id, TenantId tenantId, String name, String slug) {
    this(
        id,
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
        null);
  }

  public Outlet(
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
      UUID addressId) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.slug = slug;
    this.dayClosed = dayClosed;
    this.salesBlocked = salesBlocked;
    this.salesBlockReason = salesBlockReason;
    this.salesBlockedAt = salesBlockedAt;
    this.timezone = timezone;
    this.businessDayCutoff = businessDayCutoff;
    this.receiptPrintingEnabled = receiptPrintingEnabled;
    this.receiptHeaderMessage = receiptHeaderMessage;
    this.receiptFooterMessage = receiptFooterMessage;
    this.requireOpeningFloat = requireOpeningFloat;
    this.addressId = addressId;
  }

  public OutletId id() {
    return id;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public String name() {
    return name;
  }

  public String slug() {
    return slug;
  }

  public boolean dayClosed() {
    return dayClosed;
  }

  public boolean salesBlocked() {
    return salesBlocked;
  }

  public String salesBlockReason() {
    return salesBlockReason;
  }

  public Instant salesBlockedAt() {
    return salesBlockedAt;
  }

  public String timezone() {
    return timezone;
  }

  public LocalTime businessDayCutoff() {
    return businessDayCutoff;
  }

  public boolean receiptPrintingEnabled() {
    return receiptPrintingEnabled;
  }

  public String receiptHeaderMessage() {
    return receiptHeaderMessage;
  }

  public String receiptFooterMessage() {
    return receiptFooterMessage;
  }

  public boolean requireOpeningFloat() {
    return requireOpeningFloat;
  }

  public UUID addressId() {
    return addressId;
  }

  public Outlet closeDay() {
    if (this.dayClosed) return this; // already closed
    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        true,
        this.salesBlocked,
        this.salesBlockReason,
        this.salesBlockedAt,
        this.timezone,
        this.businessDayCutoff,
        this.receiptPrintingEnabled,
        this.receiptHeaderMessage,
        this.receiptFooterMessage,
        this.requireOpeningFloat,
        this.addressId);
  }

  public Outlet reopenDay() {
    if (!this.dayClosed) return this;
    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        false,
        this.salesBlocked,
        this.salesBlockReason,
        this.salesBlockedAt,
        this.timezone,
        this.businessDayCutoff,
        this.receiptPrintingEnabled,
        this.receiptHeaderMessage,
        this.receiptFooterMessage,
        this.requireOpeningFloat,
        this.addressId);
  }

  public Outlet applyConfigPatch(
      Boolean salesBlocked,
      String salesBlockReason,
      String timezone,
      java.time.LocalTime businessDayCutoff,
      Boolean receiptPrintingEnabled,
      String receiptHeaderMessage,
      String receiptFooterMessage,
      Boolean requireOpeningFloat,
      java.time.Instant when) {
    boolean newSalesBlocked = salesBlocked == null ? this.salesBlocked : salesBlocked;
    String newSalesBlockReason =
        salesBlockReason == null ? this.salesBlockReason : salesBlockReason;
    java.time.Instant newSalesBlockedAt =
        newSalesBlocked ? (when == null ? this.salesBlockedAt : when) : null;
    String newTimezone = timezone == null ? this.timezone : timezone;
    java.time.LocalTime newCutoff =
        businessDayCutoff == null ? this.businessDayCutoff : businessDayCutoff;
    boolean newReceiptPrintingEnabled =
        receiptPrintingEnabled == null ? this.receiptPrintingEnabled : receiptPrintingEnabled;
    String newHeader =
        receiptHeaderMessage == null ? this.receiptHeaderMessage : receiptHeaderMessage;
    String newFooter =
        receiptFooterMessage == null ? this.receiptFooterMessage : receiptFooterMessage;
    boolean newRequireOpeningFloat =
        requireOpeningFloat == null ? this.requireOpeningFloat : requireOpeningFloat;

    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        this.dayClosed,
        newSalesBlocked,
        newSalesBlockReason,
        newSalesBlockedAt,
        newTimezone,
        newCutoff,
        newReceiptPrintingEnabled,
        newHeader,
        newFooter,
        newRequireOpeningFloat,
        this.addressId);
  }

  public Outlet withSalesBlocked(boolean blocked, String reason, Instant when) {
    if (this.salesBlocked == blocked
        && ((this.salesBlockReason == null && reason == null)
            || (this.salesBlockReason != null && this.salesBlockReason.equals(reason))))
      return this;
    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        this.dayClosed,
        blocked,
        reason,
        when,
        this.timezone,
        this.businessDayCutoff,
        this.receiptPrintingEnabled,
        this.receiptHeaderMessage,
        this.receiptFooterMessage,
        this.requireOpeningFloat,
        this.addressId);
  }

  public Outlet withReceiptSettings(boolean enabled, String header, String footer) {
    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        this.dayClosed,
        this.salesBlocked,
        this.salesBlockReason,
        this.salesBlockedAt,
        this.timezone,
        this.businessDayCutoff,
        enabled,
        header,
        footer,
        this.requireOpeningFloat,
        this.addressId);
  }

  public Outlet withRequireOpeningFloat(boolean requireOpeningFloat) {
    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        this.dayClosed,
        this.salesBlocked,
        this.salesBlockReason,
        this.salesBlockedAt,
        this.timezone,
        this.businessDayCutoff,
        this.receiptPrintingEnabled,
        this.receiptHeaderMessage,
        this.receiptFooterMessage,
        requireOpeningFloat,
        this.addressId);
  }

  public Outlet withAddressId(java.util.UUID addressId) {
    return new Outlet(
        this.id,
        this.tenantId,
        this.name,
        this.slug,
        this.dayClosed,
        this.salesBlocked,
        this.salesBlockReason,
        this.salesBlockedAt,
        this.timezone,
        this.businessDayCutoff,
        this.receiptPrintingEnabled,
        this.receiptHeaderMessage,
        this.receiptFooterMessage,
        this.requireOpeningFloat,
        addressId);
  }

  public static Outlet createNew(TenantId tenantId, String name, String slug, OutletId newId) {
    return new Outlet(newId, tenantId, name, slug);
  }
}
