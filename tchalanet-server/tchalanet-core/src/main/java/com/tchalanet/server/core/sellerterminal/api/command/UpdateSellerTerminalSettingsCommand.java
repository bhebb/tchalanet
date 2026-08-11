package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import jakarta.annotation.Nullable;

public record UpdateSellerTerminalSettingsCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    SellerTerminalReceiptSettingsView receipt,
    SellerTerminalNotificationSettingsView notifications,
    @Nullable UserId actorUserId)
    implements Command<Void> {}
