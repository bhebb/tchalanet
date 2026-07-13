package com.tchalanet.server.core.draw.internal.domain.model;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lifecycle behavior of the {@code Draw} aggregate: each transition method sets its
 * status + timestamp and refuses illegal source states (via {@code DrawStatusTransition}).
 * This is where the settle precondition is proven — settle is legal ONLY from RESULTED.
 */
@DisplayName("Draw aggregate lifecycle")
class DrawTest {

    private static final Instant T1 = Instant.parse("2026-07-09T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-09T14:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-09T15:00:00Z");
    private static final Instant T4 = Instant.parse("2026-07-09T16:00:00Z");

    private static Draw scheduledDraw() {
        return Draw.scheduled(
            DrawId.of(UUID.randomUUID()),
            TenantId.of(UUID.randomUUID()),
            DrawChannelId.of(UUID.randomUUID()),
            LocalDate.of(2026, 7, 9),
            T2,  // scheduledAt (the draw time)
            T1); // cutoffAt — must be before scheduledAt
    }

    @Nested
    @DisplayName("happy path SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED")
    class HappyPath {

        @Test
        @DisplayName("each step advances the status and records its timestamp")
        void fullLifecycle() {
            Draw draw = scheduledDraw();
            assertThat(draw.status()).isEqualTo(DrawStatus.SCHEDULED);

            draw.open(T1);
            assertThat(draw.status()).isEqualTo(DrawStatus.OPEN);
            assertThat(draw.openedAt()).isEqualTo(T1);

            draw.close(T2);
            assertThat(draw.status()).isEqualTo(DrawStatus.CLOSED);
            assertThat(draw.closedAt()).isEqualTo(T2);

            DrawResultId resultId = DrawResultId.of(UUID.randomUUID());
            draw.applyResult(resultId, T3, DrawSource.MANUAL);
            assertThat(draw.status()).isEqualTo(DrawStatus.RESULTED);
            assertThat(draw.resultedAt()).isEqualTo(T3);
            assertThat(draw.drawResultId()).isEqualTo(resultId);

            draw.settle(T4);
            assertThat(draw.status()).isEqualTo(DrawStatus.SETTLED);
            assertThat(draw.settledAt()).isEqualTo(T4);
        }
    }

    @Nested
    @DisplayName("settle precondition")
    class SettlePrecondition {

        @Test
        @DisplayName("settle is legal from RESULTED")
        void settleFromResulted() {
            Draw draw = resultedDraw();
            assertThatCode(() -> draw.settle(T4)).doesNotThrowAnyException();
            assertThat(draw.status()).isEqualTo(DrawStatus.SETTLED);
        }

        @Test
        @DisplayName("settle from CLOSED (no result yet) is rejected by the state machine")
        void settleFromClosedRejected() {
            Draw draw = scheduledDraw();
            draw.open(T1);
            draw.close(T2);
            assertThatThrownBy(() -> draw.settle(T4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("SETTLED");
        }

        @Test
        @DisplayName("settle twice is rejected (SETTLED has no outgoing transition to SETTLED)")
        void settleTwiceRejected() {
            Draw draw = resultedDraw();
            draw.settle(T4);
            assertThatThrownBy(() -> draw.settle(T4))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("applyResult guards")
    class ApplyResultGuards {

        @Test
        @DisplayName("applyResult from OPEN (skipping close) is rejected")
        void applyResultFromOpenRejected() {
            Draw draw = scheduledDraw();
            draw.open(T1);
            assertThatThrownBy(() ->
                draw.applyResult(DrawResultId.of(UUID.randomUUID()), T3, DrawSource.MANUAL))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("applying a result twice is rejected by the RESULTED→RESULTED transition guard")
        void applyResultTwiceRejected() {
            Draw draw = resultedDraw();
            // NOTE: the transition check (RESULTED→RESULTED illegal) fires BEFORE the
            // `drawResultId != null` guard, so re-applying throws IllegalStateException,
            // not DrawInvalidResultException. That inner guard is effectively unreachable
            // via applyResult (see test-plan §5). We assert the real behavior.
            assertThatThrownBy(() ->
                draw.applyResult(DrawResultId.of(UUID.randomUUID()), T3, DrawSource.MANUAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESULTED -> RESULTED");
        }
    }

    @Nested
    @DisplayName("no illegal skips or repeats")
    class IllegalTransitions {

        @Test
        @DisplayName("opening an already-open draw is rejected")
        void openTwiceRejected() {
            Draw draw = scheduledDraw();
            draw.open(T1);
            assertThatThrownBy(() -> draw.open(T1))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("closing a scheduled draw (skipping open) is rejected")
        void closeWithoutOpenRejected() {
            Draw draw = scheduledDraw();
            assertThatThrownBy(() -> draw.close(T2))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    private static Draw resultedDraw() {
        Draw draw = scheduledDraw();
        draw.open(T1);
        draw.close(T2);
        draw.applyResult(DrawResultId.of(UUID.randomUUID()), T3, DrawSource.MANUAL);
        return draw;
    }
}
