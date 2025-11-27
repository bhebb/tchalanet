package com.tchalanet.server.uslottery.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tch.results")
public class ResultProviderProperties {

  private String mode = "disabled"; // fake | api | api_then_fake | disabled
  private String nyEndpoint = "https://data.ny.gov/resource/5tj6-3j5x.json";
  private String flEndpoint = "https://www.flalottery.com/api/";

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getNyEndpoint() {
    return nyEndpoint;
  }

  public void setNyEndpoint(String nyEndpoint) {
    this.nyEndpoint = nyEndpoint;
  }

  public String getFlEndpoint() {
    return flEndpoint;
  }

  public void setFlEndpoint(String flEndpoint) {
    this.flEndpoint = flEndpoint;
  }
}
