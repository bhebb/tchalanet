package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import java.util.List;

public interface OpsSchedulerHistoryProvider {

  Snapshot snapshot();

  record Snapshot(
      long failedCount,
      long staleCount,
      long neverRunCount,
      boolean historyAvailable,
      List<Item> items) {}

  record Item(
      String jobKey,
      String displayName,
      String scope,
      String status,
      String severity,
      String detailsPath,
      String context) {}
}
