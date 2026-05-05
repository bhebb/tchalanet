package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration pour le module notification (edge gateway + flows).
 */
@Configuration
@EnableConfigurationProperties({
    EdgeNotificationProperties.class,
    NotificationFlowProperties.class
})
class NotificationConfig {

    @Bean
    RestClient edgeNotificationClient(
        EdgeNotificationProperties props,
        RestClient.Builder builder
    ) {
        var factory = new JdkClientHttpRequestFactory();

        if (props.readTimeout() != null) {
            factory.setReadTimeout(props.readTimeout());
        }

        return builder
            .baseUrl(props.baseUrl())
            .requestFactory(factory)
            .build();
    }
}

