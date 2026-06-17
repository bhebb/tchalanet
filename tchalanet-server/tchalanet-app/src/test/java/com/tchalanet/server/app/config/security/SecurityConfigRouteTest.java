package com.tchalanet.server.app.config.security;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigRouteTest.TestSecurityConfig.class)
class SecurityConfigRouteTest {

    @Autowired
    WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply(springSecurity())
            .build();
    }

    // §1.2: public paths accessible without token

    @Test
    void publicPath_withoutToken_succeeds() throws Exception {
        mockMvc.perform(get("/public/ping")).andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_withoutToken_succeeds() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // §1.2: protected paths require authentication only (no business-rule authority checks)

    @Test
    void tenantPath_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/tenant/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPath_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void platformPath_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/platform/ping")).andExpect(status().isUnauthorized());
    }

    // ── test support ──────────────────────────────────────────────────────────

    @RestController
    static class TestEndpoints {
        @GetMapping("/public/ping")
        ResponseEntity<Void> pub() { return ResponseEntity.ok().build(); }

        @GetMapping("/actuator/health")
        ResponseEntity<Void> health() { return ResponseEntity.ok().build(); }

        @GetMapping("/tenant/ping")
        ResponseEntity<Void> tenant() { return ResponseEntity.ok().build(); }

        @GetMapping("/admin/ping")
        ResponseEntity<Void> admin() { return ResponseEntity.ok().build(); }

        @GetMapping("/platform/ping")
        ResponseEntity<Void> platform() { return ResponseEntity.ok().build(); }
    }

    /**
     * Mirrors the authorizeHttpRequests rules from SecurityConfig without registering
     * custom filters (which have no registered order and would cause a Spring Security error).
     * Tests the routing policy only — filter pipeline ordering is covered separately.
     */
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain security(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                    auth ->
                        auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
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
                                "/public/**")
                            .permitAll()
                            .requestMatchers("/error", "/api/v1/error")
                            .permitAll()
                            .requestMatchers(
                                "/api/v1/admin/ops",
                                "/api/v1/admin/ops/**",
                                "/admin/ops",
                                "/admin/ops/**")
                            .permitAll()
                            .anyRequest()
                            .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(withDefaults()));
            return http.build();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        TestEndpoints testEndpoints() {
            return new TestEndpoints();
        }
    }
}
