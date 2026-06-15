package com.tchalanet.server.platform.identity.internal.web;

import com.tchalanet.server.platform.identity.api.IdentityBootstrapFilter;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
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

        var user = appUserResolver.resolve(externalUser).orElse(null);

        if (user == null) {
            response.sendError(403, "external_identity.not_linked");
            return;
        }


        if (user.status() != UserStatus.ACTIVE) {
            response.sendError(403, "User not active");
            return;
        }

        if (props.updateLastLogin()) {
            appUserResolver.touchLastLogin(user.appUserId());
        }

        request.setAttribute(BOOTSTRAPPED_APP_USER_ID, user.appUserId());

        chain.doFilter(request, response);
    }

}
