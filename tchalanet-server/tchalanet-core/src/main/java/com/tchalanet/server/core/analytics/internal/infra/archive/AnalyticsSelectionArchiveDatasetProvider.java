package com.tchalanet.server.core.analytics.internal.infra.archive;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsSelectionArchiveDatasetProvider
    extends AbstractAnalyticsArchiveDatasetProvider {

  public AnalyticsSelectionArchiveDatasetProvider(NamedParameterJdbcTemplate jdbc) {
    super(jdbc, "analytics_selection", "Analytics Selection");
  }
}
