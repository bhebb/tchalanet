package com.tchalanet.server.core.draw.infra.config;

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
}
