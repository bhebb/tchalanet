package com.tchalanet.server.core.sellerterminal.internal.application.port.out;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;

public interface SellerTerminalSettingsReaderPort {
  SellerTerminalSettingsView get(TenantId tenantId, SellerTerminalId sellerTerminalId);
}
