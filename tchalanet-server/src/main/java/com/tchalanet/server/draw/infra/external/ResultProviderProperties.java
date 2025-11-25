package com.tchalanet.server.draw.infra.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tch.results")
public class ResultProviderProperties {

  private String mode = "disabled"; // fake | api | api_then_fake | disabled

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }
}
