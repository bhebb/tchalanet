package com.tchalanet.server.app.config.security;

import com.tchalanet.server.common.context.web.TchContextFilter;
import com.tchalanet.server.platform.identity.api.IdentityBootstrapFilter;
import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@Slf4j
public class SecurityConfig {

    @Value("${spring.websecurity.debug:false}")
    boolean webSecurityDebug;

    @Value("${tch.admin.server.local-open:false}")
    boolean adminServerLocalOpen;

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        JwtDecoder jwtDecoder,
        IdentityProviderApi identityProviderApi,
        IdentityBootstrapFilter userBootstrapFilter,
        TchContextFilter tchContextFilter)
        throws Exception {
        var sensitiveIdentityVerificationFilter =
            new SensitiveIdentityVerificationFilter(
                identityProviderApi, new SensitiveIdentityRequestMatcher());
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(withDefaults())
            // ✅ API stateless (pas de session)
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))

            // ✅ ne pas sauvegarder de "saved request"
            .requestCache(RequestCacheConfigurer::disable)
            .requestCache(RequestCacheConfigurer::disable)

            // ✅ on évite tout mécanisme web login
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(
                auth ->
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                        .permitAll()

                        // PUBLIC scope: fully public endpoints (health, swagger, public API)
                        .requestMatchers(
                            "/actuator/health",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/openapi/**",
                            "/api/v1/openapi/**",
                            "/api/v1/swagger-ui/**",
                            "/api/v1/public/**",
                            "/api/v1/actuator/**",
                            // also permit public endpoints without servlet prefix (helpers, local
                            // testing)
                            "/public/**")
                        .permitAll()
                        .requestMatchers("/error", "/api/v1/error")
                        .permitAll()
                        .requestMatchers("/_sdr/**", "/api/v1/_sdr/**")
                        .authenticated()

                        // Spring Boot Admin server UI/API (ops).
                        .requestMatchers(
                            "/api/v1/admin/ops",
                            "/api/v1/admin/ops/**",
                            "/admin/ops",
                            "/admin/ops/**")
                        .permitAll()

                        // PLATFORM scope: platform-level APIs (no tenant): require SUPER_ADMIN
                        .requestMatchers("/api/v1/platform/**", "/platform/**")
                        .hasAnyAuthority("SUPER_ADMIN", "ROLE_SUPER_ADMIN")

                        // ADMIN scope: tenant-administration APIs (tenant context required)
                        .requestMatchers("/api/v1/admin/**", "/admin/**")
                        .authenticated()

                        // TENANT scope: tenant business APIs (authenticated users within a tenant)
                        .requestMatchers("/api/v1/tenant/**", "/tenant/**")
                        .authenticated()

                        // Any other request must be authenticated.
                        .anyRequest()
                        .authenticated())
            .oauth2ResourceServer(
                oauth ->
                    oauth.jwt(
                        jwt ->
                            jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                    token -> convert(token, identityProviderApi))))
            .addFilterAfter(
                sensitiveIdentityVerificationFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(userBootstrapFilter, SensitiveIdentityVerificationFilter.class)
            .addFilterAfter(tchContextFilter, userBootstrapFilter.getClass());

        return http.build();
    }

    private static String asRoleAuthority(Object r) {
        if (r == null) return null;
        String s = r.toString().trim();
        if (s.isEmpty()) return null;
        return s.startsWith("ROLE_") ? s : "ROLE_" + s;
    }

    private static String asRawAuthority(Object r) {
        if (r == null) return null;
        String s = r.toString().trim();
        if (s.isEmpty()) return null;
        return s.startsWith("ROLE_") ? s.substring("ROLE_".length()) : s;
    }

    private static void addRoleAuthorities(Collection<GrantedAuthority> auths, Object role) {
        var raw = asRawAuthority(role);
        var prefixed = asRoleAuthority(role);
        if (raw != null) auths.add(new SimpleGrantedAuthority(raw));
        if (prefixed != null) auths.add(new SimpleGrantedAuthority(prefixed));
    }

    private AbstractAuthenticationToken convert(Jwt jwt, IdentityProviderApi identityProviderApi) {
        Collection<GrantedAuthority> auths = new ArrayList<>();
        var externalUser =
            identityProviderApi.mapVerifiedToken(
                new VerifiedExternalToken(
                    jwt.getClaimAsString("iss"),
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaims()),
                IdentityVerificationPolicy.STANDARD);

        // Routing hints only. TchContextFilter replaces these with DB-owned authorization.
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.forEach(role -> addRoleAuthorities(auths, role));
        }

        List<String> flatRoles = jwt.getClaimAsStringList("roles");
        if (flatRoles != null) {
            flatRoles.forEach(role -> addRoleAuthorities(auths, role));
        }

        log.warn(
            "JWT converted provider={} sub={} authorities={}",
            externalUser.provider(),
            jwt.getSubject(),
            auths
        );

        var authentication = new JwtAuthenticationToken(jwt, auths);
        authentication.setDetails(externalUser);
        return authentication;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.debug(webSecurityDebug);
    }

}
