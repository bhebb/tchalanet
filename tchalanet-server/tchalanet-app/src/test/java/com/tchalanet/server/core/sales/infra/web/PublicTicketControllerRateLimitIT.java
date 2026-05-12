package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.config.PublicTicketsRateLimitProperties;
import com.tchalanet.server.common.web.ratelimit.PublicTicketsRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style test verifying the full rate-limit behavior end-to-end via the filter.
 *
 * <p>Tests burst exhaustion (6.2) and disabled mode (6.3) without requiring a Spring context.
 * The filter is exercised directly — it's a self-contained {@code OncePerRequestFilter}.
 *
 * <p>Note: Spring Boot 4 removed {@code @WebMvcTest}/{@code @AutoConfigureMockMvc} test slices.
 * Controller-layer integration tests must be done via dedicated test profiles or E2E tests.
 */
class PublicTicketControllerRateLimitIT {

    /**
     * 6.2 — After burst exhausted (burst=5), the 6th request returns 429 with Retry-After.
     */
    @Test
    void afterBurstExhausted_returns429WithRetryAfterHeader() throws Exception {
        var props = new PublicTicketsRateLimitProperties();
        props.setEnabled(true);
        props.setRequestsPerSecond(5);
        props.setBurst(5);
        var filter = new PublicTicketsRateLimitFilter(props);

        // Exhaust 5-token burst
        for (int i = 0; i < 5; i++) {
            var req = buildRequest("/public/tickets/verify/BURST_TEST");
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).as("Request %d should pass", i + 1).isNotEqualTo(429);
        }

        // 6th request — blocked
        var req = buildRequest("/public/tickets/verify/BURST_TEST");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After"))
                .as("Retry-After header must be present on 429 response")
                .isNotNull();
    }

    /**
     * 6.3 — When rate-limit is disabled, no requests are blocked regardless of volume.
     */
    @Test
    void whenDisabled_noRequestsBlocked() throws Exception {
        var props = new PublicTicketsRateLimitProperties();
        props.setEnabled(false);
        var filter = new PublicTicketsRateLimitFilter(props);

        for (int i = 0; i < 50; i++) {
            var req = buildRequest("/public/tickets/verify/DISABLED_TEST");
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).as("Request %d should never be blocked", i + 1).isNotEqualTo(429);
        }
    }

    private MockHttpServletRequest buildRequest(String path) {
        var req = new MockHttpServletRequest("GET", path);
        req.setRemoteAddr("10.0.0.1");
        return req;
    }
}

