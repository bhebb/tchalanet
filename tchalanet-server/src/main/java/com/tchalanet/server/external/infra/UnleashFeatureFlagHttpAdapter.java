package com.tchalanet.server.external.infra;

import com.tchalanet.server.external.ports.FeatureFlagPort;
import com.tchalanet.server.featureflags.domain.model.FeatureContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnleashFeatureFlagHttpAdapter implements FeatureFlagPort {

  @Override
  public boolean isEnabled(String flagKey, FeatureContext context) {
    return false;
  }
}
