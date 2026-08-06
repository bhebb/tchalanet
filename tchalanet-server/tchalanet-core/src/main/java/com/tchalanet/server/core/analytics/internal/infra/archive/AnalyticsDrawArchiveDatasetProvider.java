package com.tchalanet.server.core.analytics.internal.infra.archive;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsDrawArchiveDatasetProvider extends AbstractAnalyticsArchiveDatasetProvider {

  public AnalyticsDrawArchiveDatasetProvider(NamedParameterJdbcTemplate jdbc) {
    super(jdbc, "analytics_draw", "Analytics Draw");
  }
}
