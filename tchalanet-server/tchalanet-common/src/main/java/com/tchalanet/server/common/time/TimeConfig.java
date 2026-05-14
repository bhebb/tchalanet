package com.tchalanet.server.common.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(TchRuntimeProperties properties) {
        return Clock.system(properties.zoneId());
    }
}
