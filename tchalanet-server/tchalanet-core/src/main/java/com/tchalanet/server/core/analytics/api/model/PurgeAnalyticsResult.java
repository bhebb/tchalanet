package com.tchalanet.server.core.analytics.api.model;

/** Result returned by {@code PurgeAnalyticsCommand}. */
public record PurgeAnalyticsResult(
    long dailyRows,
    long drawRows,
    long selectionRows,
    long sellerTerminalDrawRows,
    boolean dryRun) {

  public long totalRows() {
    return dailyRows + drawRows + selectionRows + sellerTerminalDrawRows;
  }
}
