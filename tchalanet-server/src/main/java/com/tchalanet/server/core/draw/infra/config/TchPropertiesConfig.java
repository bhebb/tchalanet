package com.tchalanet.server.core.draw.infra.config;

import com.tchalanet.server.common.config.draw.DrawResultsCommonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    DrawProperties.class,
    DrawResultsCommonProperties.class
})
public class TchPropertiesConfig {}
