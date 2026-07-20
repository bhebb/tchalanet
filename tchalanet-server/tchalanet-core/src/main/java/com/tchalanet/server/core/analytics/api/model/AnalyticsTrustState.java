package com.tchalanet.server.core.analytics.api.model;

/** Reliability state for analytics data requested by a concrete business scope. */
public enum AnalyticsTrustState {
  READY,
  RECONCILIATION_REQUIRED,
  UNAVAILABLE
}
