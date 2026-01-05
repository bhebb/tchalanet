package com.tchalanet.server.core.uslottery.infra.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties binding for US lottery providers. Mirrors YAML structure under
 * `tch.us-lottery.providers`.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tch.us-lottery")
public class UsLotteryProperties {

  private Map<String, ProviderProperties> providers;

  /** Common properties shared across providers (e.g. holidays) */
  private CommonProperties common;

  /** Default tenant id to associate external US draws when tenant not provided. */
  private String defaultTenantId = "00000000-0000-0000-0000-000000000002";

  public UUID getDefaultTenantUuid() {
    return UUID.fromString(defaultTenantId);
  }

  @Getter
  @Setter
  public static class CommonProperties {
    // list of common holidays bound from YAML, format examples: "01-01" or ISO dates
    private List<String> holidays;
  }

  @Getter
  @Setter
  public static class ProviderProperties {
    private boolean enabled = true;
    private String baseUrl;
    private String appToken;
    private String timezone;
    private String latestPath;
    private String alertPath;
    private List<GameProps> games;
    // list of holidays bound from YAML, ISO-8601 strings (e.g. 2026-01-01) or month-day strings
    private List<String> holidays;
  }

  @Getter
  @Setter
  public static class GameProps {
    private String code;
    private String externalKey;
    private String drawTime;
  }
}
