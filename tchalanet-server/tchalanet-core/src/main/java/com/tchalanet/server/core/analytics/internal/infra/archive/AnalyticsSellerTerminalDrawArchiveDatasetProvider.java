package com.tchalanet.server.core.analytics.internal.infra.archive;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsSellerTerminalDrawArchiveDatasetProvider
    extends AbstractAnalyticsArchiveDatasetProvider {

  public AnalyticsSellerTerminalDrawArchiveDatasetProvider(NamedParameterJdbcTemplate jdbc) {
    super(jdbc, "analytics_seller_terminal_draw", "Analytics Seller Terminal Draw");
  }
}
