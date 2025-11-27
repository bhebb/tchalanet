package com.tchalanet.server.featureflags.domain.ports.out;

import com.tchalanet.server.featureflags.domain.model.FeatureContext;

/** Outbound Port for integrating with a feature flag provider (e.g., Unleash, LaunchDarkly). */
public interface FeatureFlagProviderPort {
  boolean isEnabled(String flagKey, FeatureContext context);
}
