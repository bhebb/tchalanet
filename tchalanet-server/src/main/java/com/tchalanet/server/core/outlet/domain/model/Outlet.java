package com.tchalanet.server.core.outlet.domain.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

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
        if (dayClosed) return this;
        return withDayClosed(true);
    }

    public Outlet reopenDay() {
        if (!dayClosed) return this;
        return withDayClosed(false);
    }

    public Outlet blockSales(String reason, Instant when) {
        if (salesBlocked && Objects.equals(salesBlockReason, reason)) {
            return this;
        }
        
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
            receiptPrintingEnabled,
            receiptHeaderMessage,
            receiptFooterMessage,
            requireOpeningFloat,
            autoSessionOpenEnabled,
            autoSessionCloseEnabled,
            sessionOpenTime,
            sessionCloseTime,
            defaultOpeningFloatCents,
            addressId);
    }

    public Outlet unblockSales() {
        if (!salesBlocked) return this;

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
            receiptPrintingEnabled,
            receiptHeaderMessage,
            receiptFooterMessage,
            requireOpeningFloat,
            autoSessionOpenEnabled,
            autoSessionCloseEnabled,
            sessionOpenTime,
            sessionCloseTime,
            defaultOpeningFloatCents,
            addressId);
    }

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
        Boolean receiptPrintingEnabledPatch,
        String receiptHeaderMessagePatch,
        String receiptFooterMessagePatch,
        Boolean requireOpeningFloatPatch,
        Boolean autoSessionOpenEnabledPatch,
        Boolean autoSessionCloseEnabledPatch,
        LocalTime sessionOpenTimePatch,
        LocalTime sessionCloseTimePatch,
        Long defaultOpeningFloatCentsPatch,
        Instant when) {

        boolean newSalesBlocked = salesBlockedPatch == null ? salesBlocked : salesBlockedPatch;
        String newSalesBlockReason =
            salesBlockReasonPatch == null ? salesBlockReason : salesBlockReasonPatch;
        Instant newSalesBlockedAt =
            newSalesBlocked ? (when == null ? salesBlockedAt : when) : null;

        return new Outlet(
            id,
            tenantId,
            name,
            slug,
            dayClosed,
            newSalesBlocked,
            newSalesBlockReason,
            newSalesBlockedAt,
            timezonePatch == null ? timezone : timezonePatch,
            receiptPrintingEnabledPatch == null
                ? receiptPrintingEnabled
                : receiptPrintingEnabledPatch,
            receiptHeaderMessagePatch == null ? receiptHeaderMessage : receiptHeaderMessagePatch,
            receiptFooterMessagePatch == null ? receiptFooterMessage : receiptFooterMessagePatch,
            requireOpeningFloatPatch == null ? requireOpeningFloat : requireOpeningFloatPatch,
            autoSessionOpenEnabledPatch == null ? autoSessionOpenEnabled : autoSessionOpenEnabledPatch,
            autoSessionCloseEnabledPatch == null ? autoSessionCloseEnabled : autoSessionCloseEnabledPatch,
            sessionOpenTimePatch == null ? sessionOpenTime : sessionOpenTimePatch,
            sessionCloseTimePatch == null ? sessionCloseTime : sessionCloseTimePatch,
            defaultOpeningFloatCentsPatch == null
                ? defaultOpeningFloatCents
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
            receiptPrintingEnabled,
            receiptHeaderMessage,
            receiptFooterMessage,
            requireOpeningFloat,
            autoSessionOpenEnabled,
            autoSessionCloseEnabled,
            sessionOpenTime,
            sessionCloseTime,
            defaultOpeningFloatCents,
            newAddressId);
    }

    private Outlet withDayClosed(boolean newDayClosed) {
        return new Outlet(
            id,
            tenantId,
            name,
            slug,
            newDayClosed,
            salesBlocked,
            salesBlockReason,
            salesBlockedAt,
            timezone,
            receiptPrintingEnabled,
            receiptHeaderMessage,
            receiptFooterMessage,
            requireOpeningFloat,
            autoSessionOpenEnabled,
            autoSessionCloseEnabled,
            sessionOpenTime,
            sessionCloseTime,
            defaultOpeningFloatCents,
            addressId);
    }
}
