package com.tchalanet.server.common.config.draw;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw.results.shared")
public class DrawResultsCommonProperties {

  private Limits limits = new Limits();
  private Scheduler scheduler = new Scheduler();

  @Getter
  @Setter
  public static class Limits {
    private int hardMaxSlots = 200;
    private int hardDaysBack = 14;
  }

  @Getter
  @Setter
  public static class Scheduler {
    private Due due = new Due();
    private int cooldownMinutes = 20;

    @Getter
    @Setter
    public static class Due {
      private int minMinutesAfterDraw = 3;
      private int maxMinutesAfterDraw = 25;
      private int lookbackMinutes = 60;
      private int batchSize = 100;
    }
  }
}
