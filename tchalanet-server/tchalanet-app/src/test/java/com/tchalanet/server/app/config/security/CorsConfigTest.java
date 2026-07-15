package com.tchalanet.server.app.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.app.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CorsConfigTest {

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

  private static HttpServletRequest preflightFrom(String origin) {
    var request = new MockHttpServletRequest("OPTIONS", "/api/v1/public/runtime/bootstrap");
    request.addHeader("Origin", origin);
    request.addHeader("Access-Control-Request-Method", "GET");
    request.addHeader("Access-Control-Request-Headers", "authorization,x-request-id");
    return request;
  }
}
