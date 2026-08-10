package com.tchalanet.server.features.pos.profile.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdatePosProfileSettingsRequest(
    Boolean receiptAutoPrint,
    @Min(1) @Max(3) Integer receiptCopyCount,
    Boolean receiptQuickSale,
    String receiptPrinterMode,
    String receiptPaperSize,
    String receiptAdapterPreference,
    Boolean notificationsEnabled,
    Boolean notificationsCriticalOnly) {}
