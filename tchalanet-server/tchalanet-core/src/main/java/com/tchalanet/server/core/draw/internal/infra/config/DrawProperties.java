package com.tchalanet.server.core.draw.internal.infra.config;

import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw")
public class DrawProperties {

  private Cache cache = new Cache();
  private Scheduler scheduler = new Scheduler();

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
  public static class Scheduler {
    private boolean active = true;
    private ZoneId timezone = ZoneId.of("America/Port-au-Prince");
    private Generate generate = new Generate();
    private Open open = new Open();
    private Processing processing = new Processing();
  }

  @Getter
  @Setter
  public static class Generate {
    private boolean active = true;
    private String cron = "0 5 0 * * *";
    private int daysAhead = 7;
    private int maxTenantsPerRun = 1000;
  }

  @Getter
  @Setter
  public static class Open {
    private boolean active = true;
    private String cron = "0 15 0 * * *";
    private int lookaheadHours = 24;
    private int lagHours = 1;
    private int maxItemsPerRun = 10000;
  }

  @Getter
  @Setter
  public static class Processing {
    private boolean active = true;
    private String cron = "0 */5 * * * *";
    private Close close = new Close();
    private Fetch fetch = new Fetch();
    private ResultReminder resultReminder = new ResultReminder();
    private Apply apply = new Apply();
    private Settle settle = new Settle();
  }

  @Getter
  @Setter
  public static class Close {
    private boolean active = true;
    private int maxItemsPerTick = 500;
  }

  @Getter
  @Setter
  public static class DueAfterDraw {
    private boolean active = true;
    private int startMinutesAfterDraw;
    private int retryEveryMinutes;
    private int stopMinutesAfterDraw;
  }

  @Getter
  @Setter
  public static class Fetch extends DueAfterDraw {
    private int maxSlotsPerTick = 10;

    public Fetch() {
      setStartMinutesAfterDraw(5);
      setRetryEveryMinutes(10);
      setStopMinutesAfterDraw(240);
    }
  }

  @Getter
  @Setter
  public static class ResultReminder {
    private boolean active = true;
    private int manualStartMinutesAfterDraw = 5;
    private int automaticOverdueMinutesAfterDraw = 60;
    private int provisionalStuckMinutesAfterDraw = 30;
    private int maxSlotsPerTick = 25;
  }

  @Getter
  @Setter
  public static class Apply extends DueAfterDraw {
    private int maxItemsPerTick = 500;

    public Apply() {
      setStartMinutesAfterDraw(10);
      setRetryEveryMinutes(5);
      setStopMinutesAfterDraw(720);
    }
  }

  @Getter
  @Setter
  public static class Settle extends DueAfterDraw {
    private int maxItemsPerTick = 1000;

    public Settle() {
      setStartMinutesAfterDraw(10);
      setRetryEveryMinutes(5);
      setStopMinutesAfterDraw(1440);
    }
  }
}
