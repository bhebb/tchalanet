package com.tchalanet.server.core.tenant.infra.adapter;

import com.tchalanet.server.core.tenant.application.port.out.TenantStatsReaderPort;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubTenantStatsReaderAdapter implements TenantStatsReaderPort {

  @Override
  public Map<String, Object> getDashboardStats(UUID tenantId, LocalDate since) {
    var m = new HashMap<String,Object>();
    m.put("sales", 12345);
    m.put("payouts", 2345);
    m.put("active_outlets", 3);
    return m;
  }
}

