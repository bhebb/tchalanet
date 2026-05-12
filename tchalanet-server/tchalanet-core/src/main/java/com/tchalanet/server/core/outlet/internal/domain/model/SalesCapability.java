package com.tchalanet.server.core.outlet.internal.domain.model;

/**
 * Pure-domain decision returned by {@link Outlet#salesCapability()}.
 *
 * <p>Carries a verdict (allowed/blocked) and a reason code consumable by the application layer
 * without leaking domain internals.
 */
public record SalesCapability(boolean allowed, String reason) {

  public static SalesCapability allowedCapability() {
    return new SalesCapability(true, null);
  }

  public static SalesCapability blocked(String reason) {
    return new SalesCapability(false, reason);
  }
}
