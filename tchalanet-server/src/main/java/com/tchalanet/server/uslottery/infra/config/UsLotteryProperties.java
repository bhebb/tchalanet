package com.tchalanet.server.uslottery.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "tch.us-lottery")
public class UsLotteryProperties {

  /** Enable or disable calls to official US lottery APIs. */
  private boolean enabled = true;

  private String nyBaseUrl = "https://data.ny.gov/resource/5tj6-3j5x.json";

  /** Base URL for Florida lottery JSON API (to be adjusted to real endpoint). */
  private String floridaBaseUrl = "https://apim-website-prod-eastus.azure-api.net";
}
