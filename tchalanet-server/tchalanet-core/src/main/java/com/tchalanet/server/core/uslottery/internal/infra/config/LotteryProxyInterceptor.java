package com.tchalanet.server.core.uslottery.internal.infra.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

/**
 * Rewrites provider calls through the Cloudflare Worker relay. Original provider headers are
 * forwarded with the Worker's X-Fwd-* contract.
 */
final class LotteryProxyInterceptor implements ClientHttpRequestInterceptor {

  private static final Set<String> SKIPPED_HEADERS = Set.of("content-length", "host");

  private final String proxyUrl;
  private final String proxySecret;

  LotteryProxyInterceptor(String proxyUrl, String proxySecret) {
    this.proxyUrl = proxyUrl;
    this.proxySecret = proxySecret;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    var encodedTarget = URLEncoder.encode(request.getURI().toString(), StandardCharsets.UTF_8);
    var proxiedUri = URI.create(proxyUrl + "?url=" + encodedTarget);

    var originalHeaders = new HttpHeaders();
    originalHeaders.addAll(request.getHeaders());

    var wrapped =
        new HttpRequestWrapper(request) {
          @Override
          public URI getURI() {
            return proxiedUri;
          }
        };

    wrapped.getHeaders().clear();
    originalHeaders.forEach(
        (name, values) -> {
          if (!SKIPPED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            values.forEach(value -> wrapped.getHeaders().add("X-Fwd-" + name, value));
          }
        });
    wrapped.getHeaders().add("X-Proxy-Secret", proxySecret);

    return execution.execute(wrapped, body);
  }
}
