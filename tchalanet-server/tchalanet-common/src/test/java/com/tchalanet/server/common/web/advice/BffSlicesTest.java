package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.api.ServiceHealth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BffSlicesTest {

    @AfterEach
    void tearDown() {
        ApiResponseContext.clear();
    }

    @Test
    void requiredSlicePreservesBlockingExceptionFlow() {
        var failure = new IllegalStateException("stable-code-owner-exception");

        assertThatThrownBy(() -> BffSlices.required(() -> {
            throw failure;
        })).isSameAs(failure);

        assertThat(ApiResponseContext.get().getNotices()).isEmpty();
    }

    @Test
    void optionalSliceReturnsFallbackAndAddsNotice() {
        var result = BffSlices.optional(
            BffSlicePolicy.warn(
                "platform.identity.activation.error",
                "Identity activation could not be completed.",
                "platform.identity",
                NoticeSource.of("identityActivation").service("keycloak"),
                "fallback-view"
            ),
            () -> {
                throw new IllegalStateException("provider details");
            }
        );

        assertThat(result).isEqualTo("fallback-view");
        assertThat(ApiResponseContext.get().getNotices()).singleElement().satisfies(notice -> {
            assertThat(notice.code()).isEqualTo("platform.identity.activation.error");
            assertThat(notice.severity()).isEqualTo(NoticeSeverity.WARN);
            assertThat(notice.meta())
                .containsEntry("source", "identityActivation")
                .containsEntry("service", "keycloak")
                .containsKey("errorId");
        });
    }

    @Test
    void optionalSliceCanMarkDownstreamServiceDegraded() {
        BffSlices.optional(
            BffSlicePolicy.warn(
                "features.dashboard.provider_results.degraded",
                "Some provider results are temporarily unavailable.",
                "features.dashboard",
                NoticeSource.of("providerResults").service("uslottery"),
                "fallback-results"
            ).serviceStatus(ServiceHealth.DEGRADED, "Latest results unavailable"),
            () -> {
                throw new IllegalStateException("provider down");
            }
        );

        assertThat(ApiResponseContext.get().getServices()).singleElement().satisfies(service -> {
            assertThat(service.service()).isEqualTo("uslottery");
            assertThat(service.status()).isEqualTo(ServiceHealth.DEGRADED);
            assertThat(service.message()).isEqualTo("Latest results unavailable");
        });
    }
}
