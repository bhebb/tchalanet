package com.tchalanet.server.common.web.ratelimit;

import com.tchalanet.server.config.PublicTicketsRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PublicTicketsRateLimitFilter} — no Spring context.
 */
class PublicTicketsRateLimitFilterTest {

    private PublicTicketsRateLimitProperties props;
    private PublicTicketsRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        props = new PublicTicketsRateLimitProperties();
        props.setEnabled(true);
        props.setRequestsPerSecond(5);
        props.setBurst(5);
        filter = new PublicTicketsRateLimitFilter(props);
    }

    @Test
    void requestsUnderBurst_passThrough() throws Exception {
        for (int i = 0; i < 5; i++) {
            var req = buildRequest("/public/tickets/verify/ABC");
            var res = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(req, res, chain);
            assertThat(res.getStatus()).as("Request %d should pass", i + 1).isNotEqualTo(429);
        }
    }

    @Test
    void requestOverBurst_returns429WithRetryAfter() throws Exception {
        // Exhaust the burst first
        for (int i = 0; i < 5; i++) {
            var req = buildRequest("/public/tickets/verify/ABC");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        // 6th request should be blocked
        var req = buildRequest("/public/tickets/verify/ABC");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void whenDisabled_requestsAlwaysPassThrough() throws Exception {
        props.setEnabled(false);
        for (int i = 0; i < 100; i++) {
            var req = buildRequest("/public/tickets/verify/CODE");
            var res = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(req, res, chain);
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    void nonPublicTicketPath_isNotFiltered() throws Exception {
        props.setRequestsPerSecond(1);
        props.setBurst(1);
        filter = new PublicTicketsRateLimitFilter(props);
        // Exhaust the "/public/tickets" bucket
        var warmup = buildRequest("/public/tickets/verify/X");
        filter.doFilter(warmup, new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(warmup, new MockHttpServletResponse(), new MockFilterChain());

        // A different path should pass unaffected
        var otherReq = buildRequest("/api/draws");
        var res = new MockHttpServletResponse();
        filter.doFilter(otherReq, res, new MockFilterChain());
        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    void differentIps_haveSeparateBuckets() throws Exception {
        props.setRequestsPerSecond(1);
        props.setBurst(1);
        filter = new PublicTicketsRateLimitFilter(props);

        var ip1 = buildRequest("/public/tickets/verify/X", "1.1.1.1");
        var ip2 = buildRequest("/public/tickets/verify/X", "2.2.2.2");

        // Exhaust ip1
        filter.doFilter(ip1, new MockHttpServletResponse(), new MockFilterChain());
        var res1 = new MockHttpServletResponse();
        filter.doFilter(ip1, res1, new MockFilterChain());
        assertThat(res1.getStatus()).isEqualTo(429);

        // ip2 should still pass
        var res2 = new MockHttpServletResponse();
        filter.doFilter(ip2, res2, new MockFilterChain());
        assertThat(res2.getStatus()).isNotEqualTo(429);
    }

    private MockHttpServletRequest buildRequest(String path) {
        return buildRequest(path, "10.0.0.1");
    }

    private MockHttpServletRequest buildRequest(String path, String remoteAddr) {
        var req = new MockHttpServletRequest("GET", path);
        req.setRemoteAddr(remoteAddr);
        return req;
    }
}

