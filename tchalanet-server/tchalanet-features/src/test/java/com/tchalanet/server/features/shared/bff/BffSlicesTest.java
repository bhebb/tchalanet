package com.tchalanet.server.features.shared.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.NoticeKind;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.api.ServiceHealth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BffSlicesTest {

  @AfterEach
  void tearDown() {
    ApiResponseContext.clear();
  }

  @Test
  void requiredSlicePreservesBlockingExceptionFlow() {
    var failure = new IllegalStateException("stable-code-owner-exception");

    assertThatThrownBy(
            () ->
                BffSlices.required(
                    () -> {
                      throw failure;
                    }))
        .isSameAs(failure);

    assertThat(ApiResponseContext.get().getNotices()).isEmpty();
  }

  @Test
  void optionalSliceReturnsFallbackAndAddsDegradationNotice() {
    var result =
        BffSlices.optional(
            BffSlicePolicy.warn(
                    "platform.identity.activation.error",
                    "platform.identity",
                    NoticeSource.of("identityActivation").service("keycloak"),
                    "fallback-view")
                .target("identity.activation"),
            () -> {
              throw new IllegalStateException("provider details");
            });

    assertThat(result).isEqualTo("fallback-view");
    assertThat(ApiResponseContext.get().getNotices())
        .singleElement()
        .satisfies(
            notice -> {
              assertThat(notice.kind()).isEqualTo(NoticeKind.DEGRADATION);
              assertThat(notice.message()).isNull();
              assertThat(notice.target()).isEqualTo("identity.activation");
              assertThat(notice.meta())
                  .containsEntry("source", "identityActivation")
                  .containsEntry("service", "keycloak")
                  .doesNotContainKeys("target", "surface", "placement")
                  .containsKey("errorId");
            });
  }

  @Test
  void optionalSliceCanMarkAnActualDownstreamCapabilityDegraded() {
    BffSlices.optional(
        BffSlicePolicy.warn(
                "features.dashboard.provider_results.degraded",
                "features.dashboard",
                NoticeSource.of("providerResults").service("uslottery"),
                "fallback-results")
            .serviceStatus(ServiceHealth.DEGRADED),
        () -> {
          throw new IllegalStateException("provider down");
        });

    assertThat(ApiResponseContext.get().getServices())
        .singleElement()
        .satisfies(
            service -> {
              assertThat(service.service()).isEqualTo("uslottery");
              assertThat(service.status()).isEqualTo(ServiceHealth.DEGRADED);
              assertThat(service.message()).isNull();
            });
  }

  @Test
  void optionalSliceDoesNotSwallowJvmErrors() {
    var failure = new AssertionError("fatal test failure");

    assertThatThrownBy(
            () ->
                BffSlices.optional(
                    BffSlicePolicy.warn(
                        "features.dashboard.slice_unavailable",
                        "features.dashboard",
                        NoticeSource.of("dashboard"),
                        "fallback"),
                    () -> {
                      throw failure;
                    }))
        .isSameAs(failure);

    assertThat(ApiResponseContext.get().getNotices()).isEmpty();
  }
}
