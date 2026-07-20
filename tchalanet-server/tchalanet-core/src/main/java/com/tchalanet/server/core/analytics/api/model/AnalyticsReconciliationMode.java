package com.tchalanet.server.core.analytics.api.model;

/** Defines whether a reconciliation run may mutate analytics projections. */
public enum AnalyticsReconciliationMode {
  /** Compares source-of-truth snapshots to projections without changing either side. */
  VALIDATE,
  /** Rebuilds the requested tenant window from snapshots, then performs an exact validation. */
  REBUILD_AND_VALIDATE
}
