package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record UpdateSellerTerminalSettingsCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    Boolean receiptAutoPrint,
    Integer receiptCopyCount,
    Boolean receiptQuickSale,
    String receiptPrinterMode,
    String receiptPaperSize,
    String receiptAdapterPreference,
    Boolean notificationsEnabled,
    Boolean notificationsCriticalOnly,
    UserId actorUserId)
    implements Command<Void> {}
