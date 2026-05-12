package com.tchalanet.server.core.draw.internal.infra.config;

import com.tchalanet.server.core.drawresult.internal.infra.config.DrawResultsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    DrawProperties.class,
    DrawResultsProperties.class
})
public class DrawPropertiesConfig {}
