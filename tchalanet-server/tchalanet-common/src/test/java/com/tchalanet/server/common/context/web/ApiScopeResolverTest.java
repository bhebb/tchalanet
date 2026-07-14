package com.tchalanet.server.common.context.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.scope.ApiScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("ApiScopeResolver")
class ApiScopeResolverTest {

    private static MockHttpServletRequest request(String uri) {
        var req = new MockHttpServletRequest();
        req.setRequestURI(uri);
        return req;
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @ParameterizedTest
        @ValueSource(strings = {
            "/actuator/health",
            "/swagger-ui/index.html",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/openapi/v3"
        })
        @DisplayName("should return PUBLIC for infra/docs paths")
        void shouldReturnPublicForInfra(String path) {
            assertThat(ApiScopeResolver.resolve(request(path))).isEqualTo(ApiScope.PUBLIC);
        }

        @Test
        @DisplayName("should return PUBLIC for /api/v1/public/**")
        void shouldReturnPublicForPublicApi() {
            assertThat(ApiScopeResolver.resolve(request("/api/v1/public/draws")))
                .isEqualTo(ApiScope.PUBLIC);
        }

        @Test
        @DisplayName("should return IDENTITY for /api/v1/identity/**")
        void shouldReturnIdentityForIdentityApi() {
            assertThat(ApiScopeResolver.resolve(request("/api/v1/identity/me")))
                .isEqualTo(ApiScope.IDENTITY);
        }

        @Test
        @DisplayName("should return PLATFORM for /api/v1/platform/**")
        void shouldReturnPlatformForPlatformApi() {
            assertThat(ApiScopeResolver.resolve(request("/api/v1/platform/tenants")))
                .isEqualTo(ApiScope.PLATFORM);
        }

        @Test
        @DisplayName("should return PLATFORM for /api/v1/admin/ops/**")
        void shouldReturnPlatformForAdminOps() {
            assertThat(ApiScopeResolver.resolve(request("/api/v1/admin/ops/health")))
                .isEqualTo(ApiScope.PLATFORM);
        }

        @Test
        @DisplayName("should return ADMIN for /api/v1/admin/** (non-ops)")
        void shouldReturnAdminForAdminApi() {
            assertThat(ApiScopeResolver.resolve(request("/api/v1/admin/draws")))
                .isEqualTo(ApiScope.ADMIN);
        }

        @Test
        @DisplayName("should return TENANT for /api/v1/tenant/**")
        void shouldReturnTenantForTenantApi() {
            assertThat(ApiScopeResolver.resolve(request("/api/v1/tenant/sell")))
                .isEqualTo(ApiScope.TENANT);
        }

        @Test
        @DisplayName("should return PLATFORM for non-api paths to avoid public-by-accident")
        void shouldReturnPlatformForNonApiPaths() {
            assertThat(ApiScopeResolver.resolve(request("/unknown/path")))
                .isEqualTo(ApiScope.PLATFORM);
        }
    }

    @Nested
    @DisplayName("tenantRequired")
    class TenantRequired {

        @Test
        @DisplayName("should be true for TENANT and ADMIN scopes")
        void shouldRequireTenantForTenantAndAdmin() {
            assertThat(ApiScopeResolver.tenantRequired(request("/api/v1/tenant/x"))).isTrue();
            assertThat(ApiScopeResolver.tenantRequired(request("/api/v1/admin/x"))).isTrue();
        }

        @Test
        @DisplayName("should be false for PUBLIC, PLATFORM, IDENTITY")
        void shouldNotRequireTenantForOthers() {
            assertThat(ApiScopeResolver.tenantRequired(request("/api/v1/public/x"))).isFalse();
            assertThat(ApiScopeResolver.tenantRequired(request("/api/v1/platform/x"))).isFalse();
            assertThat(ApiScopeResolver.tenantRequired(request("/api/v1/identity/x"))).isFalse();
        }
    }
}
