package com.tchalanet.server.config;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Value("${app.security.required-audience}")
  private String requiredAudience;

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
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
                    .requestMatchers("/api/platform/*")
                    .hasRole("SUPER_ADMIN")
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("ADMIN_ENTERPRISE", "SUPER_ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(this::convert).decoder(jwtDecoder())));
    ;
    return http.build();
  }

  private AbstractAuthenticationToken convert(Jwt jwt) {
    // Authorities depuis realm roles et client roles (optionnel)
    Collection<GrantedAuthority> auths = new ArrayList<>();
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
      roles.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
    }
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

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:http://localhost:4200}") String origins) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(origins.split(",")));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    var s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }

  @Bean
  JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder =
        JwtDecoders.fromIssuerLocation(
            System.getProperty(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                // fallback si lancé en tests
                "http://localhost:8080/realms/tchalanet"));
    // audience validator
    OAuth2TokenValidator<Jwt> audValidator =
        jwt ->
            Optional.ofNullable(jwt.getAudience()).orElse(List.of()).contains(requiredAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        "invalid_token", "Missing required audience: " + requiredAudience, null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), audValidator));
    return decoder;
  }
}
