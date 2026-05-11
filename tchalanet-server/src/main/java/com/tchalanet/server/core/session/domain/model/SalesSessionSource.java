package com.tchalanet.server.core.session.domain.model;

/**
 * Origin of a {@link SalesSession}. Aligned with V100 CHECK constraint.
 *
 * <ul>
 *   <li>{@code MANUAL} — opened/closed by a cashier
 *   <li>{@code SCHEDULER} — auto-opened/closed by draw lifecycle scheduler
 *   <li>{@code OPS} — opened/closed by ops/admin override
 * </ul>
 */
public enum SalesSessionSource {
  MANUAL,
  SCHEDULER,
  OPS
}
