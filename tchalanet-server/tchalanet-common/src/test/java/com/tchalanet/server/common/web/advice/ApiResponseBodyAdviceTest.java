package com.tchalanet.server.common.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiStatus;
import com.tchalanet.server.common.web.api.NoticeKind;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiResponseBodyAdviceTest {

  private final ApiResponseBodyAdvice advice = new ApiResponseBodyAdvice();

  @AfterEach
  void tearDown() {
    ApiResponseContext.clear();
  }

  @Test
  void wrapsRawBodyWithContextNotice() {
    ApiResponseContext.get()
        .addNotice(
            "features.dashboard.partial",
            "Partial data",
            "features.dashboard",
            NoticeSeverity.WARN);

    var result = (ApiResponse<?>) invokeAdvice("payload");

    assertThat(result.status()).isEqualTo(ApiStatus.SUCCESS_WITH_WARNINGS);
    assertThat(result.data()).isEqualTo("payload");
    assertThat(result.notices())
        .extracting(ApiNotice::code)
        .containsExactly("features.dashboard.partial");
  }

  @Test
  void enrichesExistingApiResponseWithContextNotice() {
    var existing = ApiResponse.success("payload");
    ApiResponseContext.get()
        .addNotice(
            "features.dashboard.partial",
            "Partial data",
            "features.dashboard",
            NoticeSeverity.WARN);

    var result = (ApiResponse<?>) invokeAdvice(existing);

    assertThat(result.status()).isEqualTo(ApiStatus.SUCCESS_WITH_WARNINGS);
    assertThat(result.data()).isEqualTo("payload");
    assertThat(result.notices())
        .extracting(ApiNotice::code)
        .containsExactly("features.dashboard.partial");
  }

  @Test
  void preservesExistingApiResponseNoticeAndAddsContextNotice() {
    var existing =
        ApiResponse.warn(
            "payload", List.of(ApiNotice.warn("core.sales.limit.warning", "Limit warning")));
    ApiResponseContext.get()
        .addNotice(
            "features.dashboard.partial",
            "Partial data",
            "features.dashboard",
            NoticeSeverity.WARN);

    var result = (ApiResponse<?>) invokeAdvice(existing);

    assertThat(result.status()).isEqualTo(ApiStatus.SUCCESS_WITH_WARNINGS);
    assertThat(result.notices())
        .extracting(ApiNotice::code)
        .containsExactly("core.sales.limit.warning", "features.dashboard.partial");
  }

  @Test
  void reportsPartialWhenAnOptionalSliceIsUnavailable() {
    ApiResponseNotices.degradation(
        "tenantadmin.dashboard.analytics_unavailable",
        "features.tenantadmin",
        NoticeSeverity.WARN,
        null,
        new IllegalStateException("database connection refused"),
        java.util.Map.of("target", "analytics"));

    var result = (ApiResponse<?>) invokeAdvice("payload");

    assertThat(result.status()).isEqualTo(ApiStatus.PARTIAL);
    assertThat(result.notices())
        .singleElement()
        .satisfies(
            notice -> {
              assertThat(notice.code()).isEqualTo("tenantadmin.dashboard.analytics_unavailable");
              assertThat(notice.kind()).isEqualTo(NoticeKind.DEGRADATION);
              assertThat(notice.message()).isNull();
              assertThat(notice.target()).isEqualTo("analytics");
              assertThat(notice.params()).isEmpty();
              assertThat(notice.meta()).doesNotContainKey("exception");
              assertThat(notice.meta()).containsEntry("target", "analytics");
            });
  }

  @Test
  void preservesExplicitPendingOverWarnings() {
    var pending =
        ApiResponse.pending(
            ApiNotice.warn("sales.approval_required", "Approval required"), "ticket");
    ApiResponseContext.get()
        .addNotice("sales.limit_warning", "Limit warning", "sales", NoticeSeverity.WARN);

    var result = (ApiResponse<?>) invokeAdvice(pending);

    assertThat(result.status()).isEqualTo(ApiStatus.PENDING);
  }

  @Test
  void preservesExplicitPartialWhenNoDegradationNoticeIsPresent() {
    var result =
        (ApiResponse<?>) invokeAdvice(ApiResponse.partial("payload", List.of(), List.of()));

    assertThat(result.status()).isEqualTo(ApiStatus.PARTIAL);
  }

  @Test
  void doesNotInferPendingFromTheLegacyApprovalCode() {
    var result =
        (ApiResponse<?>)
            invokeAdvice(
                ApiResponse.warn(
                    "ticket", ApiNotice.warn("APPROVAL_REQUIRED", "Legacy approval notice")));

    assertThat(result.status()).isEqualTo(ApiStatus.SUCCESS_WITH_WARNINGS);
  }

  @Test
  void mergesTheSameContextAndResponseNoticeOnlyOnce() {
    var notice = ApiNotice.warn("sales.limit_warning", "Limit warning");
    ApiResponseContext.get().addNotice(notice);

    var result = (ApiResponse<?>) invokeAdvice(ApiResponse.warn("ticket", notice));

    assertThat(result.notices()).containsExactly(notice);
  }

  @Test
  void runsBeforeTraceAdviceToAvoidNestedApiResponseData() {
    var order =
        AnnotationUtils.findAnnotation(
            ApiResponseBodyAdvice.class, org.springframework.core.annotation.Order.class);

    assertThat(order).isNotNull();
    assertThat(order.value()).isLessThan(200);
  }

  private Object invokeAdvice(Object body) {
    return advice.beforeBodyWrite(
        body,
        null,
        MediaType.APPLICATION_JSON,
        MappingJackson2HttpMessageConverter.class,
        new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/test")),
        new ServletServerHttpResponse(new MockHttpServletResponse()));
  }
}
