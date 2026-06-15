package com.tchalanet.server.platform.identity.internal.web;

import static com.tchalanet.server.common.context.ContextKeys.BOOTSTRAPPED_APP_USER_ID;
import static com.tchalanet.server.common.context.TchContextRequestAttributes.BOOTSTRAPPED_ACTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.persistence.SellerTerminalExternalIdentityPort;
import com.tchalanet.server.platform.identity.internal.service.AppUserIdentityResolution;
import com.tchalanet.server.platform.identity.internal.service.AppUserBootstrapMode;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityAppUserResolver;
import com.tchalanet.server.platform.identity.internal.service.SellerTerminalIdentityResolution;
import com.tchalanet.server.platform.identity.internal.service.SellerTerminalIdentityResolution.TerminalBootstrapStatus;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UserBootstrapFilterImplTest {

    private final ExternalIdentityAppUserResolver appUserResolver =
        org.mockito.Mockito.mock(ExternalIdentityAppUserResolver.class);
    private final SellerTerminalExternalIdentityPort terminalPort =
        org.mockito.Mockito.mock(SellerTerminalExternalIdentityPort.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── AppUser path ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(IdentityProviderType.class)
    void bootstrapsEveryProviderThroughTheSameAppUserResolutionPath(IdentityProviderType provider)
        throws Exception {
        var externalSubject = "provider-subject";
        var appUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authenticatedWith(provider, externalSubject));
        when(appUserResolver.resolve(any(ExternalAuthenticatedUser.class)))
            .thenReturn(Optional.of(new AppUserIdentityResolution(appUserId, UserStatus.ACTIVE)));
        when(terminalPort.findByExternalIdentity(any(), any(), any())).thenReturn(Optional.empty());

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(BOOTSTRAPPED_APP_USER_ID)).isEqualTo(appUserId);
        var actor = (BootstrappedActor) request.getAttribute(BOOTSTRAPPED_ACTOR);
        assertThat(actor).isNotNull();
        assertThat(actor.actorType()).isEqualTo(TchActorType.APP_USER);
        assertThat(actor.appUserId().value()).isEqualTo(appUserId);
        assertThat(actor.sellerTerminalId()).isNull();
        verify(appUserResolver, never()).touchLastLogin(appUserId);
    }

    @Test
    void activeAppUser_setsBootstrappedActorWithCorrectProviderFields() throws Exception {
        var appUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
            authenticatedWith(IdentityProviderType.FIREBASE, "sub-abc"));
        when(appUserResolver.resolve(any())).thenReturn(
            Optional.of(new AppUserIdentityResolution(appUserId, UserStatus.ACTIVE)));
        when(terminalPort.findByExternalIdentity(any(), any(), any())).thenReturn(Optional.empty());

        var request = new MockHttpServletRequest();
        filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var actor = (BootstrappedActor) request.getAttribute(BOOTSTRAPPED_ACTOR);
        assertThat(actor.provider()).isEqualTo("FIREBASE");
        assertThat(actor.issuer()).isEqualTo("https://auth.example/realms/tchalanet");
        assertThat(actor.externalSubject()).isEqualTo("sub-abc");
    }

    @Test
    void disabledAppUser_returns403_doesNotCheckTerminal() throws Exception {
        var appUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
            authenticatedWith(IdentityProviderType.KEYCLOAK, "sub-suspended"));
        when(appUserResolver.resolve(any())).thenReturn(
            Optional.of(new AppUserIdentityResolution(appUserId, UserStatus.SUSPENDED)));

        var response = new MockHttpServletResponse();
        filter().doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        verify(terminalPort, never()).findByExternalIdentity(any(), any(), any());
    }

    @Test
    void unknownIdentity_noAppUserNoTerminal_returns403WithStableCode() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            authenticatedWith(IdentityProviderType.FIREBASE, "unlinked-subject"));
        when(appUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(terminalPort.findByExternalIdentity(any(), any(), any())).thenReturn(Optional.empty());

        var response = new MockHttpServletResponse();
        filter().doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("external_identity.not_linked");
    }

    @Test
    void rejectsAuthenticatedRequestWithoutProviderNeutralIdentityDetails() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("principal", "n/a", List.of()));
        var response = new MockHttpServletResponse();

        filter().doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        verify(appUserResolver, never()).resolve(any());
    }

    // ── SellerTerminal path ───────────────────────────────────────────────────

    @Test
    void activeSellerTerminal_setsBootstrappedActorSellerTerminal() throws Exception {
        var terminalId = SellerTerminalId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
            authenticatedWith(IdentityProviderType.FIREBASE, "terminal-sub"));
        when(appUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(terminalPort.findByExternalIdentity(any(), any(), any())).thenReturn(
            Optional.of(new SellerTerminalIdentityResolution(terminalId, tenantId, TerminalBootstrapStatus.ACTIVE)));

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        var actor = (BootstrappedActor) request.getAttribute(BOOTSTRAPPED_ACTOR);
        assertThat(actor).isNotNull();
        assertThat(actor.actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
        assertThat(actor.sellerTerminalId()).isEqualTo(terminalId);
        assertThat(actor.tenantId()).isEqualTo(tenantId);
        assertThat(actor.appUserId()).isNull();
        assertThat(request.getAttribute(BOOTSTRAPPED_APP_USER_ID)).isNull();
    }

    @Test
    void blockedSellerTerminal_returns403() throws Exception {
        var terminalId = SellerTerminalId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
            authenticatedWith(IdentityProviderType.FIREBASE, "blocked-terminal-sub"));
        when(appUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(terminalPort.findByExternalIdentity(any(), any(), any())).thenReturn(
            Optional.of(new SellerTerminalIdentityResolution(terminalId, tenantId, TerminalBootstrapStatus.BLOCKED)));

        var response = new MockHttpServletResponse();
        filter().doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("terminal.not_active");
    }

    @Test
    void disabledSellerTerminal_returns403() throws Exception {
        var terminalId = SellerTerminalId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
            authenticatedWith(IdentityProviderType.FIREBASE, "disabled-terminal-sub"));
        when(appUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(terminalPort.findByExternalIdentity(any(), any(), any())).thenReturn(
            Optional.of(new SellerTerminalIdentityResolution(terminalId, tenantId, TerminalBootstrapStatus.DISABLED)));

        var response = new MockHttpServletResponse();
        filter().doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserBootstrapFilterImpl filter() {
        return new UserBootstrapFilterImpl(appUserResolver, properties(), terminalPort);
    }

    private static UsernamePasswordAuthenticationToken authenticatedWith(
        IdentityProviderType provider, String subject) {
        var authentication =
            UsernamePasswordAuthenticationToken.authenticated("principal", "n/a", List.of());
        authentication.setDetails(
            new ExternalAuthenticatedUser(
                provider,
                "https://auth.example/realms/tchalanet",
                subject,
                "cashier@example.com",
                true,
                Map.of()));
        return authentication;
    }

    private static UserBootstrapProperties properties() {
        return new UserBootstrapProperties(
            true, false, AppUserBootstrapMode.DENY, List.of(), List.of(), false);
    }
}
