package com.tchalanet.server.core.uslottery.internal.infra.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

/**
 * Rewrites requests to go through the Cloudflare Worker relay
 * (tchalanet-infra/cloudflare-worker-proxy) for providers blocked at the datacenter/ASN level
 * when called directly from the staging host. Original headers are forwarded under an
 * {@code X-Fwd-} prefix, per the Worker's contract.
 */
final class LotteryProxyInterceptor implements ClientHttpRequestInterceptor {

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
        (name, values) -> values.forEach(value -> wrapped.getHeaders().add("X-Fwd-" + name, value)));
    wrapped.getHeaders().add("X-Proxy-Secret", proxySecret);

    return execution.execute(wrapped, body);
  }
}
