package com.tchalanet.server.core.limitpolicy.api.query;

import java.util.List;

/**
 * Effective limits resolved at TENANT and DRAW_CHANNEL scopes for a draw.
 *
 * <p>Never includes SELLER_TERMINAL resolution — callers not contextualized to a terminal must not
 * receive terminal-specific limits.
 */
public record EffectiveLimitsForDrawView(List<ResolvedRule> rules) {

  public record ResolvedRule(String ruleKey, String resolvedScope, boolean limitConfigured) {}

  public boolean hasExposureLimit() {
    return rules.stream()
        .anyMatch(
            r ->
                limitConfigured(r)
                    && r.ruleKey().equals("MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW"));
  }

  private static boolean limitConfigured(ResolvedRule r) {
    return r.limitConfigured();
  }
}
