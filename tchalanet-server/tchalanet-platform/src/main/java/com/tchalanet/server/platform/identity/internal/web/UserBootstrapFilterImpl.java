package com.tchalanet.server.platform.identity.internal.web;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityBootstrapFilter;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.persistence.SellerTerminalExternalIdentityPort;
import com.tchalanet.server.platform.identity.internal.service.AppUserIdentityResolution;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityAppUserResolver;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.tchalanet.server.common.context.ContextKeys.BOOTSTRAPPED_APP_USER_ID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserBootstrapFilterImpl extends OncePerRequestFilter implements IdentityBootstrapFilter {

    private final ExternalIdentityAppUserResolver appUserResolver;
    private final UserBootstrapProperties props;
    private final SellerTerminalExternalIdentityPort sellerTerminalPort;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain)
        throws ServletException, IOException {

        if (!props.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        if (!(auth.getDetails() instanceof ExternalAuthenticatedUser externalUser)) {
            response.sendError(403, "Missing verified external identity");
            return;
        }

        // 1. Try AppUser resolution
        var appUserResolution = appUserResolver.resolve(externalUser).orElse(null);
        if (appUserResolution != null) {
            if (appUserResolution.status() != UserStatus.ACTIVE) {
                response.sendError(403, "User not active");
                return;
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
            chain.doFilter(request, response);
            return;
        }

        // 2. Try SellerTerminal resolution (real lookup deferred to seller-terminal-v0)
        var terminalResolution = sellerTerminalPort.findByExternalIdentity(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()
        ).orElse(null);

        if (terminalResolution != null) {
            if (!terminalResolution.isActive()) {
                response.sendError(403, "terminal.not_active");
                return;
            }
            var bootstrappedActor = BootstrappedActor.sellerTerminal(
                terminalResolution.sellerTerminalId(),
                terminalResolution.tenantId(),
                externalUser.provider().name(),
                externalUser.issuer(),
                externalUser.subject()
            );
            request.setAttribute(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR, bootstrappedActor);
            chain.doFilter(request, response);
            return;
        }

        // 3. Identity not linked to any actor
        response.sendError(403, "external_identity.not_linked");
    }
}
