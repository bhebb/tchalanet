package com.tchalanet.server.core.analytics.api.model;

/** Terminal state of one analytics reconciliation run. */
public enum AnalyticsReconciliationStatus {
  /** Every observed projection matches the immutable source snapshot exactly. */
  SUCCESS,
  /** At least one projection differs from source-of-truth data. */
  MISMATCH,
  /** A repair was requested but failed to produce an exact match on the second validation. */
  REPAIR_FAILED
}
