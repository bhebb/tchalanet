package com.tchalanet.server.core.analytics.api.model;

/**
 * Dimension axis for {@code analytics_daily} projections.
 *
 * <ul>
 *   <li>{@link #PLATFORM} — global rollup across all tenants; {@code tenant_id = null}.</li>
 *   <li>{@link #TENANT}   — per-tenant KPI row.</li>
 *   <li>{@link #OUTLET}   — per-outlet breakdown inside a tenant.</li>
 *   <li>{@link #SELLER}   — per-seller/user breakdown inside a tenant.</li>
 *   <li>{@link #SELLER_TERMINAL} — per seller-terminal/POS breakdown inside a tenant.</li>
 *   <li>{@link #GAME}     — per-game breakdown inside a tenant.</li>
 *   <li>{@link #DRAW_CHANNEL} — per-draw-channel breakdown.</li>
 * </ul>
 */
public enum AnalyticsDimensionType {
  PLATFORM,
  TENANT,
  OUTLET,
  SELLER,
  SELLER_TERMINAL,
  GAME,
  DRAW_CHANNEL
}
