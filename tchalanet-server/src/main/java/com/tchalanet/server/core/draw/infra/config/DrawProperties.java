package com.tchalanet.server.core.draw.infra.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw")
public class DrawProperties {

  private Generation generation = new Generation();
  private Cache cache = new Cache();
  private Lifecycle lifecycle = new Lifecycle();
  private Watchdog watchdog = new Watchdog();
  private Settle settle = new Settle();

  @Getter
  @Setter
  public static class Generation {
    private int days = 14;
    private String cron = "0 5 0 * * *";
  }

  @Getter
  @Setter
  public static class Cache {
    private Ttl ttl = new Ttl();

    @Getter
    @Setter
    public static class Ttl {
      private int last7m = 5;
      private int todaym = 5;
      private int nexts = 60;
    }
  }

  @Getter
  @Setter
  public static class Lifecycle {
    private int batchSize = 5000;
    private int lookaheadHours = 24;
    private int lagHours = 12;
  }

  @Getter
  @Setter
  public static class Watchdog {
    private int provisionalStuckMinutes = 30;
    private String provisionalCron = "0 */15 * * * *";
  }

  @Getter
  @Setter
  public static class Settle {
    private String cron = "0 */5 * * * *";
    private List<String> providers = List.of("NY", "FL", "GA", "TX", "TN");
    private int daysBack = 1;
    private int defaultMaxDraws = 900;
    private Map<String, Integer> maxDrawsByProvider = defaultMaxDrawsByProvider();

    private static Map<String, Integer> defaultMaxDrawsByProvider() {
      var defaults = new LinkedHashMap<String, Integer>();
      defaults.put("NY", 700);
      defaults.put("FL", 900);
      defaults.put("GA", 900);
      defaults.put("TX", 900);
      defaults.put("TN", 700);
      return defaults;
    }
  }
}
