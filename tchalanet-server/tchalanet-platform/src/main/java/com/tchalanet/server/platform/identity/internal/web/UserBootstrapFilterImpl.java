package com.tchalanet.server.platform.identity.internal.web;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityBootstrapStep;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityLookup;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityAppUserResolver;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.common.context.ContextKeys.BOOTSTRAPPED_APP_USER_ID;

/**
 * Resolves the verified external identity to a Tchalanet actor and attaches a {@link
 * BootstrappedActor} request attribute.
 *
 * <p>The actor type is selected by the {@code X-Tch-Client-Type} hint (see {@link
 * ExpectedActorTypeResolver}): {@code POS} resolves a SellerTerminal, otherwise an AppUser. There is
 * <strong>no fallback</strong> between the two resolvers — a POS request with no terminal mapping is
 * denied; it does not try the AppUser resolver (and vice versa). This avoids a wasted DB lookup for
 * terminal requests and keeps actor resolution unambiguous.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserBootstrapFilterImpl implements IdentityBootstrapStep {

    private final ExternalIdentityAppUserResolver appUserResolver;
    private final SellerTerminalIdentityLookup sellerTerminalIdentityLookup;
    private final ExpectedActorTypeResolver expectedActorTypeResolver;
    private final UserBootstrapProperties props;

    @Override
    public void bootstrap(HttpServletRequest request) {
        if (!props.enabled()) {
            return;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return;
        }

        if (!(auth.getDetails() instanceof ExternalAuthenticatedUser externalUser)) {
            throw ProblemRest.forbidden("external_identity.missing_verified_identity");
        }

        if (expectedActorTypeResolver.resolve(request) == TchActorType.SELLER_TERMINAL) {
            bootstrapSellerTerminal(request, externalUser);
            return;
        }

        bootstrapAppUser(request, externalUser);
    }

    private void bootstrapAppUser(HttpServletRequest request, ExternalAuthenticatedUser externalUser) {
        var appUserResolution = appUserResolver.resolve(externalUser)
            .orElseThrow(() -> ProblemRest.forbidden("external_identity.not_linked"));

        if (appUserResolution.status() != UserStatus.ACTIVE) {
            throw ProblemRest.forbidden("user.not_active");
        }

        if (props.updateLastLogin()) {
            appUserResolver.touchLastLogin(appUserResolution.appUserId());
        }

        var bootstrappedActor = BootstrappedActor.appUser(
            UserId.of(appUserResolution.appUserId()),
            externalUser.provider().name(),
            externalUser.issuer(),
            externalUser.subject()
        );

        request.setAttribute(BOOTSTRAPPED_APP_USER_ID, appUserResolution.appUserId());
        request.setAttribute(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR, bootstrappedActor);
    }

    private void bootstrapSellerTerminal(
        HttpServletRequest request,
        ExternalAuthenticatedUser externalUser
    ) {
        var terminalResolution = sellerTerminalIdentityLookup.findByExternalIdentity(
            externalUser.provider(),
            externalUser.issuer(),
            externalUser.subject()
        ).orElseThrow(() -> ProblemRest.forbidden("terminal.external_identity_not_linked"));

        if (!terminalResolution.isActive()) {
            throw ProblemRest.forbidden("terminal.not_active");
        }

        var bootstrappedActor = BootstrappedActor.sellerTerminal(
            terminalResolution.sellerTerminalId(),
            terminalResolution.tenantId(),
            externalUser.provider().name(),
            externalUser.issuer(),
            externalUser.subject()
        );

        request.setAttribute(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR, bootstrappedActor);
    }
}
