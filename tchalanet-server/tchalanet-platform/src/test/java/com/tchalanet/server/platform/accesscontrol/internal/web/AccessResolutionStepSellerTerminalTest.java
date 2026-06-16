package com.tchalanet.server.platform.accesscontrol.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.accesscontrol.internal.service.AccessControlSnapshotResolver;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AccessResolutionStepSellerTerminalTest {

    private static final SellerTerminalId TERMINAL = SellerTerminalId.of(UUID.randomUUID());
    private static final TenantId TENANT = TenantId.of(UUID.randomUUID());

    @Mock private AccessControlSnapshotResolver snapshotResolver;
    @Mock private EffectiveTenantResolver effectiveTenantResolver;

    private AccessResolutionStepImpl step;

    @BeforeEach
    void setUp() {
        step = new AccessResolutionStepImpl(snapshotResolver, effectiveTenantResolver);
    }

    private static BootstrappedActor terminalActor() {
        return BootstrappedActor.sellerTerminal(
            TERMINAL, TENANT, "FIREBASE", "https://issuer", "sub-123");
    }

    @Test
    void sellerTerminal_tenantFromActor_withTerminalPermissions() {
        var resolved = step.resolveSellerTerminal(new MockHttpServletRequest(), terminalActor());

        assertThat(resolved.actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
        assertThat(resolved.effectiveTenantId()).isEqualTo(TENANT);
        assertThat(resolved.sellerTerminalId()).isEqualTo(TERMINAL);
        assertThat(resolved.permissionKeys()).isEqualTo(AccessResolutionStepImpl.TERMINAL_PERMISSIONS);
        assertThat(resolved.permissionKeys())
            .contains("terminal.me.read", "terminal.sell",
                "terminal.ticket.read_own", "terminal.ticket.reprint_own");
    }

    @Test
    void sellerTerminal_withTenantIdHeader_isDenied() {
        var request = new MockHttpServletRequest();
        request.addHeader(TchHeaders.X_TENANT_ID, UUID.randomUUID().toString());

        assertThatThrownBy(() -> step.resolveSellerTerminal(request, terminalActor()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("terminal.tenant_selection_not_allowed");
    }

    @Test
    void sellerTerminal_withTenantOverrideHeader_isDenied() {
        var request = new MockHttpServletRequest();
        request.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, UUID.randomUUID().toString());

        assertThatThrownBy(() -> step.resolveSellerTerminal(request, terminalActor()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("terminal.tenant_selection_not_allowed");
    }
}
