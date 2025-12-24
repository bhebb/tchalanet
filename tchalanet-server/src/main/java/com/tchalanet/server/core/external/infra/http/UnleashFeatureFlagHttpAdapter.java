package com.tchalanet.server.core.external.infra.http;

import com.tchalanet.server.core.external.port.out.FeatureFlagPort;
import com.tchalanet.server.core.featureflags.domain.model.FeatureContext;
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
