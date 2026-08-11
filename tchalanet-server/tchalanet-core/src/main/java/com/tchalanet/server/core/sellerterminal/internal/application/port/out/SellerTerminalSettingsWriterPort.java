package com.tchalanet.server.core.sellerterminal.internal.application.port.out;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import jakarta.annotation.Nullable;

public interface SellerTerminalSettingsWriterPort {
  void save(
      TenantId tenantId,
      SellerTerminalId sellerTerminalId,
      SellerTerminalSettingsView settings,
      @Nullable UserId actorUserId);
}
