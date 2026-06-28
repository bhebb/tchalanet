package com.tchalanet.server.catalog.drawchannel.api;

import com.tchalanet.server.catalog.drawchannel.api.model.ProvisioningTenantGameRef;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

public interface DrawChannelProvisioningApi {

  void ensureDefaultHaitiLotteryChannels(
      TenantId tenantId,
      List<ProvisioningTenantGameRef> tenantGames);
}
