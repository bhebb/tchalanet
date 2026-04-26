package com.tchalanet.server.core.drawresult.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw.results.fetch")
public class DrawResultsProperties {

  private boolean active = true;

  // fetch specific properties (URLs, headers, etc. can be added here)
  private Providers providers = new Providers();
  private Http http = new Http();

  @Getter
  @Setter
  public static class Providers {
    // URLs por provider added via application.yaml
  }

  @Getter
  @Setter
  public static class Http {
    private int timeoutMs = 5000;
  }
}
