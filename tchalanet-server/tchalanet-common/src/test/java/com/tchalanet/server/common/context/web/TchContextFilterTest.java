package com.tchalanet.server.common.context.web;

import static com.tchalanet.server.common.constant.ContextKeys.BOOTSTRAPPED_APP_USER_ID;
import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;
import static com.tchalanet.server.common.constant.TchHeaders.X_TCH_OVERRIDE_REASON;
import static com.tchalanet.server.common.constant.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.ActorContextResolver;
import com.tchalanet.server.common.context.AuthContextExtractor;
import com.tchalanet.server.common.context.TchContextBinder;
import com.tchalanet.server.common.context.TchContextProperties;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TenantContextInfo;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.context.tenant.TenantContextResolver;
import com.tchalanet.server.common.security.Permissions;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class TchContextFilterTest {

    private static final UUID TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    private final TchContextFilter filter = new TchContextFilter(
        new TchContextProperties("public"),
        new TenantContextResolver(new FakeTenantContextLookup()),
        new ActorContextResolver(),
        new TchRequestContextFactory(new AuthContextExtractor()),
        new TchContextBinder(),
        new OperationalContextHeaderParser());

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonSuperAdminCannotUseTenantOverrideHeader() throws Exception {
        var request = request(Map.of(X_TCH_TENANT_OVERRIDE, TENANT_UUID.toString()));
        var response = new CapturingResponse();
        var chain = new CapturingChain();

        filter.doFilterInternal(request.proxy(), response.proxy(), chain.proxy());

        assertThat(response.status).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.message).isEqualTo("Super-admin override header forbidden");
        assertThat(chain.called).isFalse();
    }

    @Test
    void superAdminOverrideRequiresReason() throws Exception {
        authenticateSuperAdmin(Permissions.Platform.TENANT_OVERRIDE);
        var request = request(Map.of(X_TCH_TENANT_OVERRIDE, TENANT_UUID.toString()));
        request.attributes.put(BOOTSTRAPPED_APP_USER_ID, USER_UUID);
        var response = new CapturingResponse();
        var chain = new CapturingChain();

        filter.doFilterInternal(request.proxy(), response.proxy(), chain.proxy());

        assertThat(response.status).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.message).isEqualTo("Super-admin override reason required");
        assertThat(chain.called).isFalse();
    }

    @Test
    void superAdminOverrideRequiresPermission() throws Exception {
        authenticateSuperAdmin();
        var request = request(Map.of(
            X_TCH_TENANT_OVERRIDE, TENANT_UUID.toString(),
            X_TCH_OVERRIDE_REASON, "support case"));
        request.attributes.put(BOOTSTRAPPED_APP_USER_ID, USER_UUID);
        var response = new CapturingResponse();
        var chain = new CapturingChain();

        filter.doFilterInternal(request.proxy(), response.proxy(), chain.proxy());

        assertThat(response.status).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.message).isEqualTo("Super-admin tenant override permission required");
        assertThat(chain.called).isFalse();
    }

    @Test
    void validSuperAdminOverrideIsRequestScopedAndBindsContext() throws Exception {
        authenticateSuperAdmin(Permissions.Platform.TENANT_OVERRIDE);
        var request = request(Map.of(
            X_TCH_TENANT_OVERRIDE, TENANT_UUID.toString(),
            X_TCH_OVERRIDE_REASON, "support case"));
        request.attributes.put(BOOTSTRAPPED_APP_USER_ID, USER_UUID);
        var response = new CapturingResponse();
        var chain = new CapturingChain();

        filter.doFilterInternal(request.proxy(), response.proxy(), chain.proxy());

        assertThat(response.status).isNull();
        assertThat(chain.called).isTrue();

        var ctx = (TchRequestContext) request.attributes.get(REQUEST_CONTEXT);
        assertThat(ctx.tenantOverridden()).isTrue();
        assertThat(ctx.tenantOverrideReason()).isEqualTo("support case");
        assertThat(ctx.effectiveTenantIdOrNull()).isEqualTo(TenantId.of(TENANT_UUID));
        assertThat(ctx.superAdminOverrideRequired().actorUserId().value()).isEqualTo(USER_UUID);
    }

    private static RequestProxy request(Map<String, String> headers) {
        return new RequestProxy(headers);
    }

    private static void authenticateSuperAdmin(String... extraRoles) {
        var roles = new java.util.ArrayList<String>();
        roles.add("SUPER_ADMIN");
        roles.addAll(List.of(extraRoles));

        var jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", USER_UUID.toString())
            .claim("roles", roles)
            .build();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, "token", List.of()));
    }

    private static final class RequestProxy {
        private final Map<String, String> headers;
        private final Map<String, Object> attributes = new HashMap<>();

        private RequestProxy(Map<String, String> headers) {
            this.headers = headers;
        }

        HttpServletRequest proxy() {
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "getHeader" -> headers.get((String) args[0]);
                case "getAttribute" -> attributes.get((String) args[0]);
                case "setAttribute" -> {
                    attributes.put((String) args[0], args[1]);
                    yield null;
                }
                case "removeAttribute" -> {
                    attributes.remove((String) args[0]);
                    yield null;
                }
                case "getRequestURI" -> "/api/v1/platform/support";
                case "getRemoteAddr" -> "127.0.0.1";
                case "getLocale" -> Locale.CANADA;
                default -> defaultValue(method.getReturnType());
            };

            return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                handler);
        }
    }

    private static final class CapturingResponse {
        private Integer status;
        private String message;

        HttpServletResponse proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("sendError".equals(method.getName())) {
                    status = (Integer) args[0];
                    message = args.length > 1 ? (String) args[1] : null;
                    return null;
                }

                return defaultValue(method.getReturnType());
            };

            return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                handler);
        }
    }

    private static final class CapturingChain {
        private boolean called;

        FilterChain proxy() {
            return (request, response) -> called = true;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }

        if (returnType == boolean.class) {
            return false;
        }

        if (returnType == int.class) {
            return 0;
        }

        if (returnType == long.class) {
            return 0L;
        }

        return null;
    }

    private static final class FakeTenantContextLookup implements TenantContextLookup {

        @Override
        public Optional<TenantContextInfo> findById(TenantId tenantId) {
            if (TenantId.of(TENANT_UUID).equals(tenantId)) {
                return Optional.of(new TenantContextInfo(
                    TenantId.of(TENANT_UUID),
                    Currency.getInstance("CAD"),
                    ZoneId.of("America/Toronto")));
            }

            return Optional.empty();
        }

        @Override
        public Optional<TenantContextInfo> findByCode(String tenantCode) {
            return Optional.empty();
        }
    }
}
