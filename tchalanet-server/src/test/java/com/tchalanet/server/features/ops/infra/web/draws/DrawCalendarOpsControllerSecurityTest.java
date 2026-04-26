package com.tchalanet.server.features.ops.infra.web.draws;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de sécurité pour {@link DrawCalendarOpsController}.
 *
 * <p>Vérifie que les endpoints {@code /platform/ops/draws} renvoient 401
 * pour toute requête sans authentification.
 */
@WebMvcTest(DrawCalendarOpsController.class)
class DrawCalendarOpsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandBus commandBus;

    @MockitoBean
    private BatchGate batchGate;

    // ── Security ─────────────────────────────────────────────────────────────

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
            return http.build();
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void generateDraws_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/platform/ops/draws/generate")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{}"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void closeDueDraws_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/platform/ops/draws/close-due")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{}"))
               .andExpect(status().isUnauthorized());
    }
}

