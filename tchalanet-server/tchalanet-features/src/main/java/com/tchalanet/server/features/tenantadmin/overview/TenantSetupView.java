package com.tchalanet.server.features.tenantadmin.overview;

import java.util.List;

/**
 * Setup progression and CTA gate returned inside {@code GET /admin/overview}.
 *
 * {@code canCreateSellerTerminal} is the authoritative backend gate:
 * identity + address + games_pricing + draws must all be non-MISSING.
 * UNKNOWN sections (checks not yet wired) do not block.
 */
public record TenantSetupView(
    int totalSteps,
    int completedSteps,
    String status,
    boolean canCreateSellerTerminal,
    List<String> blockingSteps,
    String nextRecommendedStep
) {

  public static TenantSetupView unknown() {
    return new TenantSetupView(0, 0, "INCOMPLETE", false, List.of(), null);
  }
}
