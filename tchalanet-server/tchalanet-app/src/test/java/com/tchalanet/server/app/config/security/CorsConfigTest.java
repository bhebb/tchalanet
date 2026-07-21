package com.tchalanet.server.app.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.app.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CorsConfigTest {

  private static final List<String> FIRST_PARTY_ALLOWED_HEADERS =
      List.of(
          "Authorization",
          "Content-Type",
          "Accept",
          "Origin",
          "Idempotency-Key",
          "X-Request-Id",
          "X-Tenant-Id",
          "X-Deleted-Visibility",
          "X-Tch-Tenant-Override",
          "X-Tch-Act-As",
          "X-Tch-Act-As-Terminal",
          "X-Tch-Override-Reason",
          "X-Tch-Client-Type",
          "X-Tch-Surface",
          "X-Tch-Operational-Source",
          "X-Tch-Terminal-Id",
          "X-Tch-Outlet-Id",
          "X-Tch-Sales-Session-Id");

  @Test
  void shouldAllowLocalhostWildcardOriginsWithCredentials() {
    var source =
        new CorsConfig()
            .corsConfigurationSource(
                new AppProperties(
                    "test",
                    "v1",
                    "/api",
                    URI.create("http://localhost:8083"),
                    URI.create("https://auth.localtest.me"),
                    new AppProperties.Cors(
                        List.of("http://localhost:*,https://app.localtest.me"),
                        List.of("GET", "POST", "OPTIONS"),
                        List.of("Authorization", "Content-Type", "X-Request-Id"),
                        List.of("Location"),
                        true)));

    var config = source.getCorsConfiguration(preflightFrom("http://localhost:4302"));

    assertThat(config).isNotNull();
    assertThat(config.checkOrigin("http://localhost:4302")).isEqualTo("http://localhost:4302");
    assertThat(config.getAllowCredentials()).isTrue();
  }

  @Test
  void shouldAllowFirstPartyApplicationHeaders() {
    var source =
        new CorsConfig()
            .corsConfigurationSource(
                new AppProperties(
                    "test",
                    "v1",
                    "/api",
                    URI.create("http://localhost:8083"),
                    URI.create("https://auth.localtest.me"),
                    new AppProperties.Cors(
                        List.of("http://localhost:*,https://app.localtest.me"),
                        List.of("GET", "POST", "OPTIONS"),
                        FIRST_PARTY_ALLOWED_HEADERS,
                        List.of("Location"),
                        true)));

    var config =
        source.getCorsConfiguration(
            preflightWithHeaders(
                "http://localhost:4302",
                "authorization,content-type,idempotency-key,x-tch-act-as-terminal,x-tch-client-type,"
                    + "x-tch-sales-session-id,x-deleted-visibility"));

    assertThat(config).isNotNull();
    assertThat(
            config.checkHeaders(
                List.of(
                    "authorization",
                    "content-type",
                    "idempotency-key",
                    "x-tch-act-as-terminal",
                    "x-tch-client-type",
                    "x-tch-sales-session-id",
                    "x-deleted-visibility")))
        .containsExactly(
            "authorization",
            "content-type",
            "idempotency-key",
            "x-tch-act-as-terminal",
            "x-tch-client-type",
            "x-tch-sales-session-id",
            "x-deleted-visibility");
  }

  private static HttpServletRequest preflightFrom(String origin) {
    return preflightWithHeaders(origin, "authorization,x-request-id");
  }

  private static HttpServletRequest preflightWithHeaders(String origin, String headers) {
    var request = new MockHttpServletRequest("OPTIONS", "/api/v1/public/runtime/bootstrap");
    request.addHeader("Origin", origin);
    request.addHeader("Access-Control-Request-Method", "GET");
    request.addHeader("Access-Control-Request-Headers", headers);
    return request;
  }
}
