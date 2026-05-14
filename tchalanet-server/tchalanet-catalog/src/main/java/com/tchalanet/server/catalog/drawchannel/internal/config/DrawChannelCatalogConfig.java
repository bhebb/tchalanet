package com.tchalanet.server.catalog.drawchannel.internal.config;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelDisplayFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the drawchannel catalog module.
 *
 * <p>Registers {@link DrawChannelDisplayFormatter} as a bean from internal/ so that
 * the class itself stays free of Spring annotations inside api/ (per catalog layer rules).
 */
@Configuration
public class DrawChannelCatalogConfig {

    @Bean
    public DrawChannelDisplayFormatter drawChannelDisplayFormatter() {
        return new DrawChannelDisplayFormatter();
    }
}

