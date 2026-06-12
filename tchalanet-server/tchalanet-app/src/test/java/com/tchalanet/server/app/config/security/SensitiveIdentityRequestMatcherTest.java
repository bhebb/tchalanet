package com.tchalanet.server.app.config.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveIdentityRequestMatcherTest {

  private final SensitiveIdentityRequestMatcher matcher = new SensitiveIdentityRequestMatcher();

  @ParameterizedTest
  @CsvSource({
    "POST, /api/v1/tenant/sales/tickets, true",
    "DELETE, /api/v1/admin/identity/users/1, true",
    "PATCH, /platform/tenants/1, true",
    "GET, /api/v1/tenant/cashier/home, false",
    "POST, /api/v1/public/login, true",
    "POST, /api/v1/_sdr/entities, true",
    "GET, /actuator/health, false"
  })
  void classifiesWritesAsSensitive(String method, String path, boolean expected) {
    var request = new MockHttpServletRequest(method, path);

    assertThat(matcher.matches(request)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"X-Tch-Tenant-Override, tenant-a", "X-Tenant-Id, tenant-id"})
  void classifiesTenantOverrideAsSensitive(String header, String value) {
    var request = new MockHttpServletRequest("GET", "/api/v1/tenant/cashier/home");
    request.addHeader(header, value);

    assertThat(matcher.matches(request)).isTrue();
  }
}
