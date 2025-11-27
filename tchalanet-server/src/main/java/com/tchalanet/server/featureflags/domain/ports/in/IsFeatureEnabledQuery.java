package com.tchalanet.server.featureflags.domain.ports.in;

import com.tchalanet.server.featureflags.domain.model.FeatureContext;

/** Inbound Port for querying the status of a feature flag. */
public interface IsFeatureEnabledQuery {
  boolean isEnabled(String flagKey, FeatureContext context);

  boolean isEnabled(String flagKey);
}
