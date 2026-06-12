package com.tchalanet.server.common.bus.observability;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.SimpleCommandBus;
import com.tchalanet.server.common.bus.SimpleQueryBus;
import com.tchalanet.server.common.observability.TchObservabilityProperties;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(TchObservabilityProperties.class)
@ConditionalOnProperty(prefix = "tch.observability", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ObservabilityBusConfig {

    @Bean
    @Primary
    public CommandBus observedCommandBus(
        SimpleCommandBus delegate,
        ObservationRegistry registry,
        TchObservabilityProperties props
    ) {
        return new ObservedCommandBus(delegate, registry, allowlist(props));
    }

    @Bean
    @Primary
    public QueryBus observedQueryBus(
        SimpleQueryBus delegate,
        ObservationRegistry registry,
        TchObservabilityProperties props
    ) {
        return new ObservedQueryBus(delegate, registry, allowlist(props));
    }

    private static Set<String> allowlist(TchObservabilityProperties props) {
        if (props.tracing() == null) {
            return Set.of();
        }
        List<String> list = props.tracing().sensitiveMessageAllowlist();
        return list != null ? Set.copyOf(list) : Set.of();
    }
}
