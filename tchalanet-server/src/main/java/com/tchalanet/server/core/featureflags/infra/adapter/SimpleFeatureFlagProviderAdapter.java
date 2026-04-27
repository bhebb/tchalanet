package com.tchalanet.server.core.featureflags.infra.adapter;

import com.tchalanet.server.core.featureflags.domain.model.FeatureContext;
import com.tchalanet.server.core.featureflags.domain.ports.out.FeatureFlagProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple, in-memory feature flag provider for demonstration. In a real application, this would
 * integrate with Unleash, LaunchDarkly, etc.
 */
@Component
@Slf4j
public class SimpleFeatureFlagProviderAdapter implements FeatureFlagProviderPort {

  @Override
  public boolean isEnabled(String flagKey, FeatureContext context) {
    // For demonstration, let's enable a few flags by default
    boolean enabled =
        switch (flagKey) {
          case "ff.ticket.sms_enabled" -> true;
          case "ff.admin.new_dashboard" -> false;
          case "ff.search.optimized" -> true;
          default -> false; // All other flags are disabled by default
        };
    log.debug(
        "Feature flag '{}' is {}. Context: {}", flagKey, enabled ? "ENABLED" : "DISABLED", context);
    return enabled;
  }
}
