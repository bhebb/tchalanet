package com.tchalanet.server.core.external.ports;

import com.tchalanet.server.core.featureflags.domain.model.FeatureContext; // Import our domain's

// FeatureContext

/**
 * Port for integrating with a feature flag provider (e.g., Unleash). This interface is now aligned
 * with the FeatureFlagProviderPort from the featureflags domain.
 */
public interface FeatureFlagPort {
  boolean isEnabled(String flagKey, FeatureContext context);
}
