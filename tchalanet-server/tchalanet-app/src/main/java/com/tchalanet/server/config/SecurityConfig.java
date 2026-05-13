package com.tchalanet.server.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.tchalanet.server.common.context.TchContextFilter;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapFilter;
import jakarta.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

  @Value("${app.security.required-audience}")
  private String requiredAudience;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Value("${spring.websecurity.debug:false}")
  boolean webSecurityDebug;

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      JwtDecoder jwtDecoder,
      UserBootstrapFilter userBootstrapFilter,
      TchContextFilter tchContextFilter)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(withDefaults())
        // ✅ API stateless (pas de session)
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))

        // ✅ ne pas sauvegarder de "saved request"
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
                        // also permit public endpoints without servlet prefix (helpers, local
                        // testing)
                        "/public/**",
                        "/api/v1/swagger-ui/oauth2-redirect.html")
                    .permitAll()
                    .requestMatchers("/error", "/api/v1/error")
                    .permitAll()
                    .requestMatchers("/_sdr/**", "/api/v1/_sdr/**")
                    .authenticated()

                    // PLATFORM scope: platform-level APIs (no tenant): require SUPER_ADMIN
                    .requestMatchers("/api/v1/platform/**", "/platform/**")
                    .hasRole("SUPER_ADMIN")

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
                    jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(this::convert)))
        .addFilterAfter(userBootstrapFilter, BearerTokenAuthenticationFilter.class)
        .addFilterAfter(tchContextFilter, UserBootstrapFilter.class);

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

    // Build validators list: always validate issuer, optionally validate audience when configured
    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
    if (requiredAudience != null && !requiredAudience.isBlank()) {
      validators.add(audValidator);
    }
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
    return decoder;
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

  private AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> auths = new ArrayList<>();

    // realm_access.roles (Keycloak standard)
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
      roles.forEach(role -> addRoleAuthorities(auths, role));
    }

    // root roles claim (ton cas)
    List<String> flatRoles = jwt.getClaimAsStringList("roles");
    if (flatRoles != null) {
      flatRoles.forEach(role -> addRoleAuthorities(auths, role));
    }

    return new JwtAuthenticationToken(jwt, auths);
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.debug(webSecurityDebug);
  }

  @Bean
  FilterRegistrationBean<UserBootstrapFilter> userBootstrapFilterRegistration(
      UserBootstrapFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  FilterRegistrationBean<TchContextFilter> tchContextFilterRegistration(TchContextFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
