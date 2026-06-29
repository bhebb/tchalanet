package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseNoticesTest {

    @AfterEach
    void tearDown() {
        ApiResponseContext.clear();
        MDC.clear();
    }

    @Test
    void warnAddsStandardNoticeMetadataToRequestContext() {
        MDC.put(TchTraceIds.MDC_REQUEST_ID, "req-1");
        MDC.put(TchTraceIds.MDC_TRACE_ID, "trace-1");
        MDC.put(TchTraceIds.MDC_SPAN_ID, "span-1");

        ApiResponseNotices.warn(
            "platform.identity.activation.error",
            "Identity activation could not be completed.",
            "platform.identity",
            NoticeSource.of("identityActivation")
                .service("keycloak")
                .operation("completeFirstLogin"),
            new IllegalStateException("provider failed")
        );

        var notices = ApiResponseContext.get().getNotices();

        assertThat(notices).hasSize(1);
        var notice = notices.getFirst();
        assertThat(notice.code()).isEqualTo("platform.identity.activation.error");
        assertThat(notice.domain()).isEqualTo("platform.identity");
        assertThat(notice.severity()).isEqualTo(NoticeSeverity.WARN);
        assertThat(notice.meta())
            .containsEntry("source", "identityActivation")
            .containsEntry("service", "keycloak")
            .containsEntry("operation", "completeFirstLogin")
            .containsEntry("requestId", "req-1")
            .containsEntry("traceId", "trace-1")
            .containsEntry("spanId", "span-1")
            .containsKey("errorId");
    }

    @Test
    void infoWithoutErrorDoesNotCreateErrorId() {
        ApiResponseNotices.info(
            "features.dashboard.stats.partial",
            "Some statistics are not available.",
            "features.dashboard",
            NoticeSource.of("stats")
        );

        var notice = ApiResponseContext.get().getNotices().getFirst();

        assertThat(notice.severity()).isEqualTo(NoticeSeverity.INFO);
        assertThat(notice.meta()).containsEntry("source", "stats");
        assertThat(notice.meta()).doesNotContainKey("errorId");
    }
}
