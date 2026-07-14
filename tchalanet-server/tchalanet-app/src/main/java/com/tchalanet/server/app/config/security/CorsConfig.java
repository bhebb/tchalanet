package com.tchalanet.server.app.config.security;

import com.tchalanet.server.app.config.AppProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Bean
  CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
    var cfg = new CorsConfiguration();

    // Origins autorisés (séparés par virgules dans la config).
    // setAllowedOriginPatterns supporte les wildcards (ex: http://localhost:*)
    // et reste compatible avec allowCredentials: true.
    cfg.setAllowedOriginPatterns(splitCsv(appProperties.cors().allowedOrigins()));

    // Méthodes explicitement autorisées
    cfg.setAllowedMethods(splitCsv(appProperties.cors().allowedMethods()));

    // Headers qu’on accepte (plus de "*")
    cfg.setAllowedHeaders(splitCsv(appProperties.cors().allowedHeaders()));

    // Headers qu’on expose au frontend (optionnel)
    cfg.setExposedHeaders(splitCsv(appProperties.cors().exposedHeaders()));

    // True si tu passes des cookies ou Authorization
    cfg.setAllowCredentials(appProperties.cors().allowCredentials());

    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  private static List<String> splitCsv(List<String> values) {
    if (values == null) {
      return List.of();
    }

    return values.stream()
        .flatMap(value -> List.of(value.split(",")).stream())
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
  }
}
