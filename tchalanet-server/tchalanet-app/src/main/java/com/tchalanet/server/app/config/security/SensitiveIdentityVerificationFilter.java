package com.tchalanet.server.app.config.security;

import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

final class SensitiveIdentityVerificationFilter extends OncePerRequestFilter {

  private final IdentityProviderApi identityProviderApi;
  private final RequestMatcher sensitiveRequestMatcher;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  SensitiveIdentityVerificationFilter(
      IdentityProviderApi identityProviderApi, RequestMatcher sensitiveRequestMatcher) {
    this(identityProviderApi, sensitiveRequestMatcher, new BearerTokenAuthenticationEntryPoint());
  }

  SensitiveIdentityVerificationFilter(
      IdentityProviderApi identityProviderApi,
      RequestMatcher sensitiveRequestMatcher,
      AuthenticationEntryPoint authenticationEntryPoint) {
    this.identityProviderApi = identityProviderApi;
    this.sensitiveRequestMatcher = sensitiveRequestMatcher;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (sensitiveRequestMatcher.matches(request)
        && authentication instanceof JwtAuthenticationToken jwtAuthentication
        && authentication.isAuthenticated()) {
      try {
        var jwt = jwtAuthentication.getToken();
        var externalUser =
            identityProviderApi.mapVerifiedToken(
                new VerifiedExternalToken(
                    jwt.getClaimAsString("iss"),
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaims()),
                IdentityVerificationPolicy.SENSITIVE);
        jwtAuthentication.setDetails(externalUser);
      } catch (IdentityProviderException | IllegalArgumentException ex) {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
            request, response, new BadCredentialsException("Sensitive identity check failed", ex));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
