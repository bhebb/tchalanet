package com.tchalanet.server.common.web.observability;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiTraceInfo;
import com.tchalanet.server.common.web.observability.ApiResponseTraceAdvice.ApiResponseWithTrace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.tchalanet.server.common.observability.TchTraceIds.MDC_REQUEST_ID;
import static com.tchalanet.server.common.observability.TchTraceIds.MDC_SPAN_ID;
import static com.tchalanet.server.common.observability.TchTraceIds.MDC_TRACE_ID;
import static com.tchalanet.server.common.web.observability.RequiredRequestIdFilter.ATTR_REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTraceAdviceTest {

    private final ApiResponseTraceAdvice advice = new ApiResponseTraceAdvice();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void injectsTraceWhenRequestIdPresent() {
        var req = new MockHttpServletRequest();
        req.setAttribute(ATTR_REQUEST_ID, "tch_req_abc123");

        var body = ApiResponse.success("data");
        var result = invokeAdvice(advice, body, req);

        assertThat(result).isInstanceOf(ApiResponseWithTrace.class);
        var withTrace = (ApiResponseWithTrace<?>) result;
        assertThat(withTrace.trace()).isNotNull();
        assertThat(withTrace.trace().requestId()).isEqualTo("tch_req_abc123");
        assertThat(withTrace.status()).isEqualTo(body.status());
        assertThat(withTrace.data()).isEqualTo("data");
    }

    @Test
    void injectsTraceWhenMdcTraceIdsPresent() {
        MDC.put(MDC_TRACE_ID, "abc123traceId");
        MDC.put(MDC_SPAN_ID, "def456spanId");

        var body = ApiResponse.success("data");
        var result = invokeAdvice(advice, body, new MockHttpServletRequest());

        assertThat(result).isInstanceOf(ApiResponseWithTrace.class);
        var withTrace = (ApiResponseWithTrace<?>) result;
        assertThat(withTrace.trace().traceId()).isEqualTo("abc123traceId");
        assertThat(withTrace.trace().spanId()).isEqualTo("def456spanId");
    }

    @Test
    void noTraceBlockWhenNoContextAvailable() {
        var body = ApiResponse.success("data");
        var result = invokeAdvice(advice, body, new MockHttpServletRequest());

        // No requestId attribute, no MDC traceId/spanId → body unchanged
        assertThat(result).isSameAs(body);
    }

    @Test
    void nonApiResponseBodyPassedThrough() {
        var body = "plain string";
        var result = invokeAdvice(advice, body, new MockHttpServletRequest());

        assertThat(result).isSameAs(body);
    }

    @Test
    void nullBodyPassedThrough() {
        var result = invokeAdvice(advice, null, new MockHttpServletRequest());
        assertThat(result).isNull();
    }

    @Test
    void preservesAllApiResponseFields() {
        var req = new MockHttpServletRequest();
        req.setAttribute(ATTR_REQUEST_ID, "tch_req_123");
        var original = ApiResponse.success("payload");

        var result = (ApiResponseWithTrace<?>) invokeAdvice(advice, original, req);

        assertThat(result.status()).isEqualTo(original.status());
        assertThat(result.data()).isEqualTo(original.data());
        assertThat(result.notices()).isEqualTo(original.notices());
        assertThat(result.services()).isEqualTo(original.services());
    }

    // --- helpers ---

    private static Object invokeAdvice(
        ApiResponseTraceAdvice advice,
        Object body,
        MockHttpServletRequest httpReq
    ) {
        var serverReq = new ServletServerHttpRequest(httpReq);
        var serverRes = new ServletServerHttpResponse(new MockHttpServletResponse());
        return advice.beforeBodyWrite(
            body, null, MediaType.APPLICATION_JSON,
            MappingJackson2HttpMessageConverter.class,
            serverReq, serverRes
        );
    }
}
