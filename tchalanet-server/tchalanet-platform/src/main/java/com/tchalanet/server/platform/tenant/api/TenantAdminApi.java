package com.tchalanet.server.platform.tenant.api;

import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;

public interface TenantAdminApi {

  void updateDefaultCommissionRate(TenantId tenantId, BigDecimal rate);
}
