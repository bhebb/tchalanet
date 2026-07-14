package com.tchalanet.server.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("TchContextBinder")
class TchContextBinderTest {

    private final TchContextBinder binder = new TchContextBinder();

    @AfterEach
    void cleanup() {
        TchContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("bind should set ThreadLocal, request attribute, and MDC keys")
    void bindSetsAllThreeStores() {
        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/tenant/test");
        var ctx = minimalContext();

        binder.bind(req, ctx);

        assertThat(TchContext.currentOrNull()).isSameAs(ctx);
        assertThat(req.getAttribute(ContextKeys.REQUEST_CONTEXT)).isSameAs(ctx);
        assertThat(MDC.get("tenant_effective")).isNotNull();
        assertThat(MDC.get("reqId")).isNotNull();
    }

    @Test
    @DisplayName("clear should remove only TchContext MDC keys, preserving upstream keys")
    void clearPreservesUpstreamMdcKeys() {
        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/tenant/test");

        MDC.put("traceId", "abc123");
        MDC.put("spanId", "def456");

        binder.bind(req, minimalContext());
        assertThat(MDC.get("tenant_effective")).isNotNull();

        binder.clear(req);

        assertThat(TchContext.currentOrNull()).isNull();
        assertThat(MDC.get("tenant_effective")).isNull();
        assertThat(MDC.get("reqId")).isNull();
        assertThat(MDC.get("traceId")).isEqualTo("abc123");
        assertThat(MDC.get("spanId")).isEqualTo("def456");
    }

    @Test
    @DisplayName("clear should remove ThreadLocal context")
    void clearRemovesThreadLocal() {
        var req = new MockHttpServletRequest();
        req.setRequestURI("/test");

        binder.bind(req, minimalContext());
        assertThat(TchContext.hasContext()).isTrue();

        binder.clear(req);
        assertThat(TchContext.hasContext()).isFalse();
    }

    private static TchRequestContext minimalContext() {
        var tenantId = TenantId.of(UUID.randomUUID());
        return new TchRequestContext(
            "test", tenantId.value(), "test", tenantId.value(),
            null, Set.of(), Set.of(),
            java.util.Locale.getDefault(), "req-123", "127.0.0.1", null,
            false, null, "active", ApiScope.TENANT, null,
            tenantId, ZoneId.of("UTC"), Currency.getInstance("HTG"), null,
            TchActorType.SYSTEM, null, Set.of(), Set.of(), null
        );
    }
}
