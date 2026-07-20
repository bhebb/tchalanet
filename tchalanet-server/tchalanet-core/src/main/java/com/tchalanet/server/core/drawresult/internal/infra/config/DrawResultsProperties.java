package com.tchalanet.server.core.drawresult.internal.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw.results")
public class DrawResultsProperties {

  private Limits limits = new Limits();
  private Defaults defaults = new Defaults();
  private Notifications notifications = new Notifications();

  @Getter
  @Setter
  public static class Limits {
    private int maxSlotsPerTick = 100;
    private int hardDaysBack = 7;
  }

  @Getter
  @Setter
  public static class Defaults {
    private int manualDaysBack = 0;
    private int manualMaxSlots = 50;
  }

  @Getter
  @Setter
  public static class Notifications {
    private Slack slack = new Slack();
  }

  @Getter
  @Setter
  public static class Slack {
    private boolean enabled = false;
    private String channel = "batch-draws";
    private String priority = "LOW";
  }
}
