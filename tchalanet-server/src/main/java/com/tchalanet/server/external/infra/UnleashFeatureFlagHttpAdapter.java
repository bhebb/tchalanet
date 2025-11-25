package com.tchalanet.server.external.infra;

import com.tchalanet.server.external.ports.FeatureFlagPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UnleashFeatureFlagHttpAdapter implements FeatureFlagPort {

  private final RestTemplate rest;
  private final String base = "http://localhost:4242"; // config later

  public UnleashFeatureFlagHttpAdapter(RestTemplateBuilder b) {
    this.rest = b.build();
  }

  @Override
  public boolean isEnabled(String flagName) {
    // simple check for now, to replace with a proper client
    try {
      var r = rest.getForObject(base + "/api/client/features/" + flagName, String.class);
      return r != null;
    } catch (Exception ex) {
      return false;
    }
  }
}
