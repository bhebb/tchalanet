package com.tchalanet.server.common.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

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
        NoticeSource.of("identityActivation").service("keycloak").operation("completeFirstLogin"),
        new IllegalStateException("provider failed"));

    var notices = ApiResponseContext.get().getNotices();

    assertThat(notices).hasSize(1);
    var notice = notices.getFirst();
    assertThat(notice.code()).isEqualTo("platform.identity.activation.error");
    assertThat(notice.domain()).isEqualTo("platform.identity");
    assertThat(notice.severity()).isEqualTo(NoticeSeverity.WARN);
    assertThat(notice.source())
        .isEqualTo(
            NoticeSource.of("identityActivation")
                .service("keycloak")
                .operation("completeFirstLogin"));
    assertThat(notice.target()).isNull();
    assertThat(notice.params()).isEmpty();
    assertThat(notice.trace())
        .satisfies(
            trace -> {
              assertThat(trace.requestId()).isEqualTo("req-1");
              assertThat(trace.traceId()).isEqualTo("trace-1");
              assertThat(trace.spanId()).isEqualTo("span-1");
              assertThat(trace.errorId()).isNotBlank();
            });
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
        NoticeSource.of("stats"));

    var notice = ApiResponseContext.get().getNotices().getFirst();

    assertThat(notice.severity()).isEqualTo(NoticeSeverity.INFO);
    assertThat(notice.source()).isEqualTo(NoticeSource.of("stats"));
    assertThat(notice.trace()).isNull();
    assertThat(notice.meta()).containsEntry("source", "stats");
    assertThat(notice.meta()).doesNotContainKey("errorId");
  }

  @Test
  void informationFactoryCreatesCodeFirstNoticeWithoutServerProse() {
    var notice =
        com.tchalanet.server.common.web.api.ApiNotice.information(
            "subscription.cancelled",
            "subscription",
            NoticeSource.of("subscription").operation("cancel"),
            "admin.subscription.actions",
            java.util.Map.of());

    assertThat(notice.code()).isEqualTo("subscription.cancelled");
    assertThat(notice.message()).isNull();
    assertThat(notice.target()).isEqualTo("admin.subscription.actions");
    assertThat(notice.source()).isEqualTo(NoticeSource.of("subscription").operation("cancel"));
    assertThat(notice.params()).isEmpty();
  }

  @Test
  void responseContextDeduplicatesTheSameFunctionalNotice() {
    ApiResponseNotices.degradation(
        "tenantadmin.dashboard.analytics_unavailable",
        "features.tenantadmin",
        NoticeSeverity.WARN,
        NoticeSource.of("tenantAdminDashboard"),
        new IllegalStateException("first"),
        java.util.Map.of("target", "tenantadmin.dashboard.analytics"));
    ApiResponseNotices.degradation(
        "tenantadmin.dashboard.analytics_unavailable",
        "features.tenantadmin",
        NoticeSeverity.WARN,
        NoticeSource.of("tenantAdminDashboard"),
        new IllegalStateException("second"),
        java.util.Map.of("target", "tenantadmin.dashboard.analytics"));

    assertThat(ApiResponseContext.get().getNotices()).hasSize(1);
  }

  @Test
  void degradationKeepsOnlyPublicParamsAndUsesStructuredTarget() {
    ApiResponseNotices.degradation(
        "tenantadmin.dashboard.analytics_unavailable",
        "features.tenantadmin",
        NoticeSeverity.WARN,
        NoticeSource.of("tenantAdminDashboard"),
        new IllegalStateException("database connection refused"),
        java.util.Map.of(
            "target", "tenantadmin.dashboard.analytics",
            "retryAfterSeconds", 15,
            "providerPayload", "do-not-send",
            "terminalPin", "123456",
            "nested", java.util.Map.of("not", "public")));

    var notice = ApiResponseContext.get().getNotices().getFirst();

    assertThat(notice.target()).isEqualTo("tenantadmin.dashboard.analytics");
    assertThat(notice.params()).containsEntry("retryAfterSeconds", 15).hasSize(1);
    assertThat(notice.meta())
        .containsEntry("target", "tenantadmin.dashboard.analytics")
        .doesNotContainKeys("terminalPin", "providerPayload");
  }
}
