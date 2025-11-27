package com.tchalanet.server.featureflags.application;

import com.tchalanet.server.external.ports.FeatureFlagPort;
import com.tchalanet.server.featureflags.domain.model.FeatureContext;
import com.tchalanet.server.featureflags.domain.ports.in.IsFeatureEnabledQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService implements IsFeatureEnabledQuery {

  // Now inject the FeatureFlagPort from the external domain
  private final FeatureFlagPort featureFlagPort;

  @Override
  public boolean isEnabled(String flagKey, FeatureContext context) {
    log.debug("Checking feature flag '{}' with context: {}", flagKey, context);
    return featureFlagPort.isEnabled(flagKey, context);
  }

  @Override
  public boolean isEnabled(String flagKey) {
    return true;
  }
}
