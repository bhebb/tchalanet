package com.tchalanet.server.common.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Source unique de vérité pour l'heure système dans l'application.
 *
 * <p>Utiliser ce bean partout (batch, use cases, etc.) pour faciliter les tests et garder un
 * comportement cohérent.
 */

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(TchRuntimeProperties properties) {
        return Clock.system(properties.zoneId());
    }
}
