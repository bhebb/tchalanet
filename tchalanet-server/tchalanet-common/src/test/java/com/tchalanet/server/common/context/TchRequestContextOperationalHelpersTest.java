package com.tchalanet.server.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.context.operational.OperationalContextRole;
import com.tchalanet.server.common.security.ApiScope;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TchRequestContextOperationalHelpersTest {

    private static final TenantId TENANT_ID =
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId USER_ID =
        UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final TerminalId TERMINAL_ID =
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final OutletId OUTLET_ID =
        OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final SalesSessionId SALES_SESSION_ID =
        SalesSessionId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));

    @Test
    void missingPosContextIsRejected() {
        assertThatThrownBy(() -> context(EnumSet.of(TchRole.CASHIER), null, false, null)
            .trustedPosOperationalContextRequired())
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void weakPosContextIsRejectedForTrustedHelper() {
        assertThatThrownBy(() -> context(
            EnumSet.of(TchRole.CASHIER),
            new OperationalRequestContext(
                TERMINAL_ID,
                OUTLET_ID,
                SALES_SESSION_ID,
                OperationalContextSource.CLIENT_CLAIM),
            false,
            null).trustedPosOperationalContextRequired())
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void sellerHelperReturnsSellerContextForCashier() {
        var seller = context(
            EnumSet.of(TchRole.CASHIER),
            strongContext(),
            false,
            null).sellerOperationalContextRequired();

        assertThat(seller.role()).isEqualTo(OperationalContextRole.SELLER);
        assertThat(seller.sellerUserId()).isEqualTo(USER_ID);
        assertThat(seller.terminalId()).isEqualTo(TERMINAL_ID);
    }

    @Test
    void adminHelperRejectsSellerContext() {
        assertThatThrownBy(() -> context(
            EnumSet.of(TchRole.CASHIER),
            strongContext(),
            false,
            null).adminOperationalContextRequired())
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void superAdminOverrideRequiresReason() {
        assertThatThrownBy(() -> context(
            EnumSet.of(TchRole.SUPER_ADMIN),
            null,
            true,
            null).superAdminOverrideRequired())
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void superAdminOverrideReturnsTypedContext() {
        var context = context(
            EnumSet.of(TchRole.SUPER_ADMIN),
            null,
            true,
            "support case");

        var override = context.superAdminOverrideRequired();

        assertThat(override.effectiveTenantId()).isEqualTo(TENANT_ID);
        assertThat(override.actorUserId()).isEqualTo(USER_ID);
        assertThat(override.overrideReason()).isEqualTo("support case");
    }

    private static OperationalRequestContext strongContext() {
        return new OperationalRequestContext(
            TERMINAL_ID,
            OUTLET_ID,
            SALES_SESSION_ID,
            OperationalContextSource.SIGNED_DEVICE_BINDING);
    }

    private static TchRequestContext context(
        Set<TchRole> roles,
        OperationalRequestContext operationalContext,
        boolean tenantOverridden,
        String tenantOverrideReason) {

        return new TchRequestContext(
            "demo",
            TENANT_ID.value(),
            "demo",
            TENANT_ID.value(),
            "keycloak-user",
            USER_ID.value(),
            roles,
            Set.of(),
            java.util.Locale.CANADA_FRENCH,
            "req-1",
            "127.0.0.1",
            "test",
            tenantOverridden,
            tenantOverrideReason,
            "active",
            ApiScope.TENANT,
            null,
            TENANT_ID,
            ZoneId.of("America/Toronto"),
            Currency.getInstance("CAD"),
            operationalContext);
    }
}
