package com.tchalanet.server.app.config.security;

import com.tchalanet.server.app.config.AppProperties;
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

        // Origins autorisés (séparés par virgules dans la config)
        cfg.setAllowedOrigins(appProperties.cors().allowedOrigins());

        // Méthodes explicitement autorisées
        cfg.setAllowedMethods(appProperties.cors().allowedMethods());

        // Headers qu’on accepte (plus de "*")
        cfg.setAllowedHeaders(appProperties.cors().allowedHeaders());

        // Headers qu’on expose au frontend (optionnel)
        cfg.setExposedHeaders(appProperties.cors().exposedHeaders());

        // True si tu passes des cookies ou Authorization
        cfg.setAllowCredentials(appProperties.cors().allowCredentials());

        var src = new UrlBasedCorsConfigurationSource();
        // On ne l’applique qu’aux endpoints API
        src.registerCorsConfiguration("/api/**", cfg);
        return src;
    }
}
