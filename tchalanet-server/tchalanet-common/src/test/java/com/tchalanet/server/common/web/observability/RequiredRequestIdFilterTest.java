package com.tchalanet.server.common.web.observability;

import com.tchalanet.server.common.observability.TchObservabilityProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.observability.TchTraceIds.MDC_REQUEST_ID;
import static com.tchalanet.server.common.web.observability.RequiredRequestIdFilter.ATTR_REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;

class RequiredRequestIdFilterTest {

    private static final TchObservabilityProperties PROPERTIES = new TchObservabilityProperties(
        true,
        new TchObservabilityProperties.RequestIdProperties(
            true, true, "^[A-Za-z0-9._:\\-]{8,96}$",
            List.of("/public/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**",
                "/error", "/favicon.ico")
        ),
        new TchObservabilityProperties.TracingProperties(true, List.of())
    );

    private RequiredRequestIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequiredRequestIdFilter(PROPERTIES);
        MDC.clear();
    }

    @Test
    void missingHeaderReturns400() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(400);
        assertThat(res.getContentType()).contains("application/problem+json");
        assertThat(res.getContentAsString()).contains("request_id.missing");
        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }

    @Test
    void invalidHeaderReturns400() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        req.addHeader(X_REQUEST_ID, "bad value!");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(400);
        assertThat(res.getContentAsString()).contains("request_id.invalid");
        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }

    @Test
    void validHeaderPassesAndSetsAttribute() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        req.addHeader(X_REQUEST_ID, "tch_req_01JZABCDEF1234567890");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(req.getAttribute(ATTR_REQUEST_ID)).isEqualTo("tch_req_01JZABCDEF1234567890");
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void exemptPathPassesWithoutHeader() throws Exception {
        var req = new MockHttpServletRequest("GET", "/public/results");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void actuatorPathIsExempt() throws Exception {
        var req = new MockHttpServletRequest("GET", "/actuator/health");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void optionsAlwaysExempt() throws Exception {
        var req = new MockHttpServletRequest("OPTIONS", "/api/v1/tenant/something");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void mdcClearedAfterRequest() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        req.addHeader(X_REQUEST_ID, "tch_req_01JZABCDEF1234567890");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(MDC.get(MDC_REQUEST_ID)).isNull();
    }

    @Test
    void mdcPopulatedDuringRequest() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        req.addHeader(X_REQUEST_ID, "tch_req_01JZABCDEF1234567890");
        var res = new MockHttpServletResponse();
        var captured = new String[1];

        FilterChain chain = (rq, rs) -> captured[0] = MDC.get(MDC_REQUEST_ID);

        filter.doFilter(req, res, chain);

        assertThat(captured[0]).isEqualTo("tch_req_01JZABCDEF1234567890");
    }

    @Test
    void missingHeaderProblemDetailContainsServerRequestId() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getContentAsString())
            .contains("requestId")
            .contains("srv_req_");
    }

    @Test
    void disabledObservabilityPassesAllRequests() throws Exception {
        var disabled = new TchObservabilityProperties(
            false,
            new TchObservabilityProperties.RequestIdProperties(true, true, null, null),
            new TchObservabilityProperties.TracingProperties(true, List.of())
        );
        var f = new RequiredRequestIdFilter(disabled);
        var req = new MockHttpServletRequest("GET", "/api/v1/tenant/something");
        var res = new MockHttpServletResponse();
        var chain = Mockito.mock(FilterChain.class);

        f.doFilter(req, res, chain);

        Mockito.verify(chain).doFilter(req, res);
    }
}
