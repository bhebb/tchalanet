package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Read-only queries for public content consumers (PageModel providers, dashboards). */
@Service
@RequiredArgsConstructor
public class PublicContentQueryService {

  private final PublicContentAggregationService aggregation;
  private final Clock clock;

  public List<PublicContentItem> listForSurface(PublicContentSurface surface, int limit) {
    Instant now = Instant.now(clock);
    int cap = limit <= 0 ? 5 : Math.min(limit, 50);
    return aggregation.aggregateForSurface(surface, now).stream().limit(cap).toList();
  }

  /** All surfaces (public home = no filter). */
  public List<PublicContentItem> listAll(int limit) {
    Instant now = Instant.now(clock);
    int cap = limit <= 0 ? 5 : Math.min(limit, 50);
    return aggregation.aggregateForSurface(null, now).stream().limit(cap).toList();
  }
}
