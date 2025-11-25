package com.tchalanet.server.draw.infra.usecase.impl;

import com.tchalanet.server.draw.domain.model.ChannelSummary;
import com.tchalanet.server.draw.domain.model.DrawSummary;
import com.tchalanet.server.draw.domain.usecase.GetPublicDrawsSummaryUseCase;
import com.tchalanet.server.draw.infra.cache.PublicDrawsCacheService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GetPublicDrawsSummaryUseCaseImpl implements GetPublicDrawsSummaryUseCase {

  private final PublicDrawsCacheService cache;

  public GetPublicDrawsSummaryUseCaseImpl(PublicDrawsCacheService cache) {
    this.cache = cache;
  }

  @Override
  public DrawSummary getSummaryForTenant(UUID tenantId) {
    // V1: try cache, else build a minimal mocked response
    DrawSummary s = cache.getFromCache(tenantId);
    if (s != null) return s;

    // simple mocked channels for V1
    var now = OffsetDateTime.now();
    ChannelSummary c1 =
        new ChannelSummary(
            "tch-ht-3pm",
            "Haïti 3PM",
            "SCHEDULED",
            now.plusHours(2),
            now.plusHours(1).plusMinutes(30),
            true,
            List.of());
    ChannelSummary c2 =
        new ChannelSummary(
            "tch-ht-8pm",
            "Haïti 8PM",
            "SCHEDULED",
            now.plusHours(7),
            now.plusHours(6).plusMinutes(30),
            false,
            List.of());

    var summary =
        new DrawSummary(
            tenantId, List.of(c1, c2), java.util.Map.of("generatedAt", java.time.Instant.now()));
    cache.put(tenantId, summary);
    return summary;
  }
}
