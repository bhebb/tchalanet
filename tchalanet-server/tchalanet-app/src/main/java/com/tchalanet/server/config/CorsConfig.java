package com.tchalanet.server.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String allowedOrigins,
      @Value("${app.cors.allowed-methods}") String allowedMethods,
      @Value("${app.cors.allowed-headers}") String allowedHeaders,
      @Value("${app.cors.exposed-headers}") String exposedHeaders,
      @Value("${app.cors.allow-credentials}") boolean allowCredentials) {
    var cfg = new CorsConfiguration();

    // Origins autorisés (séparés par virgules dans la config)
    cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));

    // Méthodes explicitement autorisées
    cfg.setAllowedMethods(List.of(allowedMethods.split(",")));

    // Headers qu’on accepte (plus de "*")
    cfg.setAllowedHeaders(List.of(allowedHeaders.split(",")));

    // Headers qu’on expose au frontend (optionnel)
    cfg.setExposedHeaders(List.of(exposedHeaders.split(",")));

    // True si tu passes des cookies ou Authorization
    cfg.setAllowCredentials(allowCredentials);

    var src = new UrlBasedCorsConfigurationSource();
    // On ne l’applique qu’aux endpoints API
    src.registerCorsConfiguration("/api/**", cfg);
    return src;
  }
}
