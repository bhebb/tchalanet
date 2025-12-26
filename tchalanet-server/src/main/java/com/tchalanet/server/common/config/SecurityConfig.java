package com.tchalanet.server.common.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Value("${app.security.required-audience}")
  private String requiredAudience;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Bean
  SecurityFilterChain security(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api/v1/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/v1/openapi/**",
                        "/api/v1/configs/i18n/**",
                        "/api/v1/pages/home-public")
                    .permitAll()
                    .requestMatchers("/api/platform/**")
                    .hasRole("SUPER_ADMIN")
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("TENANT_ADMIN", "SUPER_ADMIN")
                    // secure app-settings, i18n-overrides, games, themes, agents, roles,
                    // permissions, tenant-users
                    .requestMatchers(
                        "/admin/app-settings/**",
                        "/admin/i18n-overrides/**",
                        "/admin/games/**",
                        "/admin/themes/**",
                        "/admin/agents/**",
                        "/admin/roles/**",
                        "/admin/permissions/**",
                        "/admin/tenant-users/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN_TENANT")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth.jwt(
                    jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(this::convert)));

    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);

    OAuth2TokenValidator<Jwt> audValidator =
        jwt -> {
          List<String> aud = Optional.ofNullable(jwt.getAudience()).orElse(List.of());
          return aud.contains(requiredAudience)
              ? OAuth2TokenValidatorResult.success()
              : OAuth2TokenValidatorResult.failure(
                  new OAuth2Error(
                      "invalid_token", "Missing required audience: " + requiredAudience, null));
        };

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuerUri), audValidator));
    return decoder;
  }

  private AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> auths = new ArrayList<>();

    // 1. roles dans realm_access (Keycloak classique)
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
      roles.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
    }

    // 2. fallback sur le claim "roles" à la racine (ton mapper actuel)
    List<String> flatRoles = jwt.getClaimAsStringList("roles");
    if (flatRoles != null) {
      flatRoles.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
    }

    // 3. client roles (optionnel)
    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null) {
      resourceAccess.forEach(
          (clientId, obj) -> {
            if (obj instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> cr) {
              cr.forEach(r -> auths.add(new SimpleGrantedAuthority(clientId + ":" + r)));
            }
          });
    }

    return new JwtAuthenticationToken(jwt, auths);
  }
}
