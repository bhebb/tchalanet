package com.tchalanet.server.platform.identity.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityLookup;
import com.tchalanet.server.platform.identity.api.model.SellerTerminalBootstrapStatus;
import com.tchalanet.server.platform.identity.api.model.SellerTerminalIdentityBootstrapView;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.service.AppUserIdentityResolution;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityAppUserResolver;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapProperties;
import com.tchalanet.server.common.web.error.ProblemRestException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserBootstrapFilterImplTest {

    private static final ExternalAuthenticatedUser EXTERNAL_USER = new ExternalAuthenticatedUser(
        IdentityProviderType.FIREBASE, "https://issuer", "sub-123", "a@b.com", true, Map.of());

    @Mock private ExternalIdentityAppUserResolver appUserResolver;
    @Mock private SellerTerminalIdentityLookup sellerTerminalIdentityLookup;
    @Mock private UserBootstrapProperties props;

    private UserBootstrapFilterImpl filter;

    @BeforeEach
    void setUp() {
        lenient().when(props.enabled()).thenReturn(true);
        filter = new UserBootstrapFilterImpl(
            appUserResolver, sellerTerminalIdentityLookup, new ExpectedActorTypeResolver(), props);

        var auth = new TestingAuthenticationToken("principal", "creds");
        auth.setAuthenticated(true);
        auth.setDetails(EXTERNAL_USER);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── POS header → SellerTerminal resolver only ───────────────────────────────

    @Test
    void posHeader_withActiveTerminal_resolvesSellerTerminal_andSkipsAppUserResolver() {
        when(sellerTerminalIdentityLookup.findByExternalIdentity(
            IdentityProviderType.FIREBASE, "https://issuer", "sub-123"))
            .thenReturn(Optional.of(new SellerTerminalIdentityBootstrapView(
                SellerTerminalId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                SellerTerminalBootstrapStatus.ACTIVE)));

        var request = posRequest();
        filter.bootstrap(request);

        assertThat(actorOf(request).actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
        verifyNoInteractions(appUserResolver);
    }

    @Test
    void posHeader_noTerminalMapping_isDenied_withoutTryingAppUser() {
        when(sellerTerminalIdentityLookup.findByExternalIdentity(
            IdentityProviderType.FIREBASE, "https://issuer", "sub-123"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> filter.bootstrap(posRequest()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("terminal.external_identity_not_linked");
        verifyNoInteractions(appUserResolver);
    }

    @Test
    void posHeader_inactiveTerminal_isDenied() {
        when(sellerTerminalIdentityLookup.findByExternalIdentity(
            IdentityProviderType.FIREBASE, "https://issuer", "sub-123"))
            .thenReturn(Optional.of(new SellerTerminalIdentityBootstrapView(
                SellerTerminalId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                SellerTerminalBootstrapStatus.BLOCKED)));

        assertThatThrownBy(() -> filter.bootstrap(posRequest()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("terminal.not_active");
    }

    // ── No POS header → AppUser resolver only ───────────────────────────────────

    @Test
    void noPosHeader_withActiveAppUser_resolvesAppUser_andSkipsTerminalResolver() {
        when(appUserResolver.resolve(EXTERNAL_USER))
            .thenReturn(Optional.of(new AppUserIdentityResolution(UUID.randomUUID(), UserStatus.ACTIVE)));

        var request = new MockHttpServletRequest();
        filter.bootstrap(request);

        assertThat(actorOf(request).actorType()).isEqualTo(TchActorType.APP_USER);
        verifyNoInteractions(sellerTerminalIdentityLookup);
    }

    @Test
    void noPosHeader_noAppUserMapping_isDenied_withoutTryingTerminal() {
        when(appUserResolver.resolve(EXTERNAL_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filter.bootstrap(new MockHttpServletRequest()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("external_identity.not_linked");
        verifyNoInteractions(sellerTerminalIdentityLookup);
    }

    private static MockHttpServletRequest posRequest() {
        var request = new MockHttpServletRequest();
        request.addHeader(TchHeaders.X_TCH_CLIENT_TYPE, TchHeaders.CLIENT_TYPE_POS);
        return request;
    }

    private static BootstrappedActor actorOf(MockHttpServletRequest request) {
        return (BootstrappedActor)
            request.getAttribute(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR);
    }
}
