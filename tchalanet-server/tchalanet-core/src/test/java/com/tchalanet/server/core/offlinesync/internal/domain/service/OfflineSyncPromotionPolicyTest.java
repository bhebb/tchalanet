package com.tchalanet.server.core.offlinesync.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OfflineSyncPromotionPolicy")
class OfflineSyncPromotionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");

    private OfflineSubmission newValidatedSubmission(PromotionAttemptId attempt) {
        var sub = OfflineSubmission.receive(
            OfflineSubmissionId.of(UUID.randomUUID()),
            TenantId.of(UUID.randomUUID()),
            OfflineSyncBatchId.of(UUID.randomUUID()),
            OfflineGrantId.of(UUID.randomUUID()),
            OfflineCodeBatchId.of(UUID.randomUUID()),
            "C1", "client-sub-1",
            UUID.randomUUID(),
            UserId.of(UUID.randomUUID()),
            TerminalId.of(UUID.randomUUID()),
            OutletId.of(UUID.randomUUID()),
            SalesSessionId.of(UUID.randomUUID()),
            com.tchalanet.server.common.types.id.DrawId.of(UUID.randomUUID()),
            NOW.minusSeconds(60), NOW,
            new Money(new BigDecimal("10.00"), CurrencyCode.of("HTG")), 1,
            "hash-1", "sig-1"
        );
        return sub.markTechValidated(attempt, NOW);
    }

    @Nested
    @DisplayName("evaluateReturn")
    class EvaluateReturn {

        @Test
        @DisplayName("applies when incoming attempt matches the current one")
        void appliesOnMatch() {
            var attempt = PromotionAttemptId.of(UUID.randomUUID());
            var sub = newValidatedSubmission(attempt);

            var outcome = OfflineSyncPromotionPolicy.evaluateReturn(sub, attempt);

            assertThat(outcome).isInstanceOf(OfflineSyncPromotionPolicy.Outcome.Apply.class);
        }

        @Test
        @DisplayName("ignores stale event with non-matching attempt")
        void ignoresStaleAttempt() {
            var current = PromotionAttemptId.of(UUID.randomUUID());
            var stale = PromotionAttemptId.of(UUID.randomUUID());
            var sub = newValidatedSubmission(current);

            var outcome = OfflineSyncPromotionPolicy.evaluateReturn(sub, stale);

            assertThat(outcome).isInstanceOfSatisfying(
                OfflineSyncPromotionPolicy.Outcome.Ignore.class,
                ig -> assertThat(ig.reason()).contains("stale promotion attempt"));
        }

        @Test
        @DisplayName("ignores when incoming attempt is null")
        void ignoresNullIncoming() {
            var sub = newValidatedSubmission(PromotionAttemptId.of(UUID.randomUUID()));

            var outcome = OfflineSyncPromotionPolicy.evaluateReturn(sub, null);

            assertThat(outcome).isInstanceOf(OfflineSyncPromotionPolicy.Outcome.Ignore.class);
        }
    }
}
