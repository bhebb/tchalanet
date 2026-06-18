package com.tchalanet.server.platform.accesscontrol.api.permissionevaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class TchPermissionEvaluatorTest {

    // §9.2 — permission present → true

    @Test
    void permissionPresent_returnsTrue() {
        var evaluator = evaluator(contextWith(Set.of("payout:approve")));

        assertThat(evaluator.hasPermission(auth(), null, "payout:approve")).isTrue();
    }

    // §9.2 — permission absent → false

    @Test
    void permissionAbsent_returnsFalse() {
        var evaluator = evaluator(contextWith(Set.of("payout:approve")));

        assertThat(evaluator.hasPermission(auth(), null, "other.permission")).isFalse();
    }

    // §9.2 — context absent → false

    @Test
    void contextAbsent_returnsFalse() {
        var evaluator = evaluator(null);

        assertThat(evaluator.hasPermission(auth(), null, "payout:approve")).isFalse();
    }

    // §9.2 — authentication absent → false

    @Test
    void authenticationAbsent_returnsFalse() {
        var evaluator = evaluator(contextWith(Set.of("payout:approve")));

        assertThat(evaluator.hasPermission(null, null, "payout:approve")).isFalse();
    }

    // §9.2 — permission DENY: resolved context does not contain the key → false

    @Test
    void permissionNotInResolvedSet_returnsFalse() {
        var evaluator = evaluator(contextWith(Set.of("ticket.read")));

        assertThat(evaluator.hasPermission(auth(), null, "payout:approve")).isFalse();
    }

    // §9.2 — seller_terminal.sell works for active seller terminal.

    @Test
    void sellerTerminalSell_presentInPermissions_returnsTrue() {
        var evaluator = evaluator(contextWith(Set.of("seller_terminal.sell")));

        assertThat(evaluator.hasPermission(auth(), null, "seller_terminal.sell")).isTrue();
    }

    @Test
    void sellerTerminalSell_absentFromPermissions_returnsFalse() {
        var evaluator = evaluator(contextWith(Set.of()));

        assertThat(evaluator.hasPermission(auth(), null, "seller_terminal.sell")).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static TchPermissionEvaluator evaluator(TchRequestContext ctx) {
        return new TchPermissionEvaluator(new StaticContextResolver(ctx));
    }

    private static Authentication auth() {
        return new UsernamePasswordAuthenticationToken("user", "n/a", Set.of());
    }

    private static TchRequestContext contextWith(Set<String> permissionKeys) {
        var tenantId = UUID.randomUUID();
        return new TchRequestContext(
            "tenant", tenantId, "tenant", tenantId,
            "ext-user", UUID.randomUUID(),
            EnumSet.of(TchRole.TENANT_ADMIN), Set.of(),
            Locale.ENGLISH, "request-id", "127.0.0.1", "test",
            false, null, "active", ApiScope.TENANT,
            null, TenantId.of(tenantId), ZoneId.of("UTC"), Currency.getInstance("USD"),
            null,
            null, null,
            Set.of(),            // roleCodes
            permissionKeys,      // permissionKeys — the set under test
            null);
    }

    private static final class StaticContextResolver extends TchContextResolver {
        private final TchRequestContext context;

        StaticContextResolver(TchRequestContext context) {
            this.context = context;
        }

        @Override
        public TchRequestContext currentOrNull() {
            return context;
        }
    }
}
