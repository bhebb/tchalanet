package com.tchalanet.server.common.http;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public final class RestClientFactory {

  private final Duration connectTimeout;
  private final Duration readTimeout;

  public RestClientFactory() {
    // mets ça en @Value si tu veux (tch.http.*)
    this.connectTimeout = Duration.ofSeconds(5);
    this.readTimeout = Duration.ofSeconds(10);
  }

  public RestClient.Builder builder() {
    var rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) connectTimeout.toMillis());
    rf.setReadTimeout((int) readTimeout.toMillis());

    return RestClient.builder().requestFactory(rf);
  }

  public RestClient create(String baseUrl) {
    return builder().baseUrl(baseUrl).build();
  }

  public RestClient create() {
    return builder().build();
  }
}
