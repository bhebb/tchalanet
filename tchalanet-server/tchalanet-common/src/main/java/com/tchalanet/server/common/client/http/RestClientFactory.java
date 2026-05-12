package com.tchalanet.server.common.client.http;

import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public final class RestClientFactory {

    private final HttpClientProperties properties;

    public RestClient.Builder builder() {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) properties.connectTimeout().toMillis());
        rf.setReadTimeout((int) properties.readTimeout().toMillis());

        return RestClient.builder().requestFactory(rf);
    }

    public RestClient create(String baseUrl) {
        return builder().baseUrl(baseUrl).build();
    }

    public RestClient create() {
        return builder().build();
    }
}
