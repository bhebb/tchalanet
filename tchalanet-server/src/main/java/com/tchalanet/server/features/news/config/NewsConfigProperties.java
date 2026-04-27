package com.tchalanet.server.features.news.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "tch.news")
public class NewsConfigProperties {

  /** Configuration du provider de news global utilisé pour la page publique. */
  private Provider provider = new Provider();

  /** Configuration du cache global pour les news publiques. */
  private Cache cache = new Cache();

  /** TTL global des news (en heures). */
  private Ttl ttl = new Ttl();

  /** Configuration du cron de rafraîchissement global. */
  private Refresh refresh = new Refresh();

  @Getter
  @Setter
  public static class Provider {
    /** URL du flux de news (RSS/JSON). */
    private String url;

    /** Clé API éventuelle pour le provider. */
    private String apiKey;
  }

  @Getter
  @Setter
  public static class Cache {
    /** Clé de base utilisée pour le cache des news publiques (si besoin côté Redis brut). */
    private String key;

    /** Nom logique du cache Spring (Caffeine/Redis) pour les news publiques. */
    private String cacheName;
  }

  @Getter
  @Setter
  public static class Ttl {
    /** TTL des news en heures. */
    private int hours;
  }

  @Getter
  @Setter
  public static class Refresh {
    /** Expression cron de rafraîchissement des news. */
    private String cron;
  }
}
