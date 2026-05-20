package com.tchalanet.server.platform.communication.internal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class EdgeCommunicationConfig {

    @Bean("edgeCommunicationClient")
    RestClient edgeCommunicationClient(
        EdgeCommunicationProperties props,
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

