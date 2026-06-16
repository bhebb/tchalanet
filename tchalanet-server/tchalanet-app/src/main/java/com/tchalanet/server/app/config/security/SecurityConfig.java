package com.tchalanet.server.app.config.security;

import com.tchalanet.server.common.context.web.TchContextFilter;
import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@Slf4j
public class SecurityConfig {

    @Value("${spring.websecurity.debug:false}")
    boolean webSecurityDebug;

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        JwtDecoder jwtDecoder,
        IdentityProviderApi identityProviderApi,
        TchAccessContextPipelineFilter tchAccessContextPipelineFilter,
        TchContextFilter tchContextFilter
    ) throws Exception {
        var sensitiveIdentityVerificationFilter =
            new SensitiveIdentityVerificationFilter(
                identityProviderApi,
                new SensitiveIdentityRequestMatcher()
            );

        http.csrf(AbstractHttpConfigurer::disable)
            .cors(withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .requestCache(RequestCacheConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                .permitAll()

                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/openapi/**",
                    "/api/v1/openapi/**",
                    "/api/v1/swagger-ui/**",
                    "/api/v1/public/**",
                    "/api/v1/actuator/**",
                    "/public/**",
                    "/error",
                    "/api/v1/error"
                )
                .permitAll()

                .requestMatchers(
                    "/api/v1/admin/ops",
                    "/api/v1/admin/ops/**",
                    "/admin/ops",
                    "/admin/ops/**"
                )
                .permitAll()

                .anyRequest()
                .authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(token -> convert(token, identityProviderApi))
            ))

            .addFilterAfter(
                sensitiveIdentityVerificationFilter,
                BearerTokenAuthenticationFilter.class
            )
            .addFilterAfter(
                tchAccessContextPipelineFilter,
                BearerTokenAuthenticationFilter.class
            )
            .addFilterBefore(
                tchContextFilter,
                org.springframework.security.web.access.intercept.AuthorizationFilter.class
            );

        return http.build();
    }

    private AbstractAuthenticationToken convert(Jwt jwt, IdentityProviderApi identityProviderApi) {
        var externalUser =
            identityProviderApi.mapVerifiedToken(
                new VerifiedExternalToken(
                    jwt.getClaimAsString("iss"),
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaims()),
                IdentityVerificationPolicy.STANDARD);

        log.debug("jwt.convert provider={} sub={}", externalUser.provider(), jwt.getSubject());

        // Authorities are empty here; AccessResolutionFilter populates ACTOR_*, ROLE_*, PERM_*.
        var authentication = new JwtAuthenticationToken(jwt, List.of());
        authentication.setDetails(externalUser);
        return authentication;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.debug(webSecurityDebug);
    }

}
