package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.core.session.api.query.ResolvePosOperationContextQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosSaleContextResolverTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID = TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID = OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final SalesSessionId SESSION_ID =
        SalesSessionId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));

    @Test
    void validatesTerminalForSellTicketAfterResolvingTrustedPosContext() {
        var queryBus = new CapturingQueryBus();
        var resolver = new PosSaleContextResolver(queryBus);

        var result = resolver.resolve(context());

        assertThat(result.terminalId()).isEqualTo(TERMINAL_ID);
        assertThat(queryBus.terminalValidation).isNotNull();
        assertThat(queryBus.terminalValidation.tenantId()).isEqualTo(TENANT_ID);
        assertThat(queryBus.terminalValidation.terminalId()).isEqualTo(TERMINAL_ID);
        assertThat(queryBus.terminalValidation.outletId()).isEqualTo(OUTLET_ID);
        assertThat(queryBus.terminalValidation.actorUserId()).isEqualTo(USER_ID);
        assertThat(queryBus.terminalValidation.operation()).isEqualTo(TerminalOperation.SELL_TICKET);
    }

    private static TchRequestContext context() {
        return new TchRequestContext(
            "demo",
            TENANT_ID.value(),
            "demo",
            TENANT_ID.value(),
            USER_ID.value().toString(),
            USER_ID.value(),
            Set.of(TchRole.CASHIER),
            Set.of(),
            Locale.CANADA_FRENCH,
            "req-1",
            "127.0.0.1",
            "test",
            false,
            null,
            "active",
            ApiScope.TENANT,
            "idem-key",
            TENANT_ID,
            java.time.ZoneOffset.UTC,
            Currency.getInstance("USD"),
            new OperationalContextHint(
                TERMINAL_ID,
                OUTLET_ID,
                SESSION_ID,
                OperationalContextSource.SIGNED_DEVICE_BINDING,
                OperationalContextTrust.STRONG
            ),
            null, null, null, null, null
        );
    }

    private static final class CapturingQueryBus implements QueryBus {
        private ValidateTerminalForOperationQuery terminalValidation;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R ask(Query<R> query) {
            if (query instanceof ResolvePosOperationContextQuery) {
                return (R) new ValidatedPosOperationContext(
                    TENANT_ID,
                    USER_ID,
                    TERMINAL_ID,
                    OUTLET_ID,
                    SESSION_ID,
                    OperationalContextSource.SIGNED_DEVICE_BINDING,
                    OperationalContextTrust.STRONG,
                    Instant.parse("2026-05-26T10:00:00Z")
                );
            }
            if (query instanceof ValidateTerminalForOperationQuery terminalQuery) {
                terminalValidation = terminalQuery;
                return null;
            }
            throw new UnsupportedOperationException(query.getClass().getName());
        }
    }
}
