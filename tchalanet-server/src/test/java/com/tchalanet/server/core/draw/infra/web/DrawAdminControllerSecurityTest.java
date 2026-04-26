package com.tchalanet.server.core.draw.infra.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawAdminWebMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de sécurité pour {@link DrawAdminController}.
 *
 * <p>Vérifie que le scope {@code /admin/draws} :
 * <ul>
 *   <li>renvoie 401 pour toute requête sans authentification ;</li>
 *   <li>renvoie 403 pour une requête authentifiée sans l'autorité {@code SUPER_ADMIN}.</li>
 * </ul>
 */
@WebMvcTest(DrawAdminController.class)
class DrawAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandBus commandBus;

    @MockitoBean
    private QueryBus queryBus;

    @MockitoBean
    private DrawAdminWebMapper mapper;

    @MockitoBean
    private TchContextResolver contextResolver;

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
    void listDraws_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/admin/draws"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void listDraws_withInsufficientAuthority_returns403() throws Exception {
        mockMvc.perform(get("/admin/draws"))
               .andExpect(status().isForbidden());
    }
}

