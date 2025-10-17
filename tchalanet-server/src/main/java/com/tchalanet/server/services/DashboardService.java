package com.tchalanet.server.services;

import com.tchalanet.server.dto.DrawSummaryDto;
import com.tchalanet.server.dto.KpisDto;
import com.tchalanet.server.dto.TenantFeaturesDto;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

  public TenantFeaturesDto getFeatures(String tenant, String role) {
    return new TenantFeaturesDto(tenant, role, List.of("ticket.create", "draw.view", "sales.read"));
  }

  public KpisDto getKpis(String tenant, String role) {
    return new KpisDto(1245.50, 382, 4, 225.75);
  }

  public DrawSummaryDto getNextDraw(String tenant) {
    return new DrawSummaryDto("d1", "Midi", Instant.now().plusSeconds(3600), "OPEN");
  }
}
