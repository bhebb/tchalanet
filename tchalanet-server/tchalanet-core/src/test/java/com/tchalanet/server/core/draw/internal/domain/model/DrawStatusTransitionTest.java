package com.tchalanet.server.core.draw.internal.domain.model;

import com.tchalanet.server.core.draw.api.model.DrawStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * State-machine guard for the draw lifecycle. This test pins the <em>entire</em>
 * from×to matrix so any accidental widening/narrowing of the allowed transitions
 * is caught. Canonical lifecycle:
 * {@code SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED}, plus CANCELED/ARCHIVED.
 */
@DisplayName("DrawStatusTransition")
class DrawStatusTransitionTest {

    /**
     * The expected legal transitions — kept independently from production so the
     * test fails if the production map drifts, rather than mirroring the same bug.
     */
    private static final Map<DrawStatus, Set<DrawStatus>> EXPECTED_LEGAL =
        Map.of(
            DrawStatus.SCHEDULED, Set.of(DrawStatus.OPEN, DrawStatus.CANCELED),
            DrawStatus.OPEN, Set.of(DrawStatus.CLOSED, DrawStatus.CANCELED),
            DrawStatus.CLOSED, Set.of(DrawStatus.RESULTED, DrawStatus.CANCELED),
            DrawStatus.RESULTED, Set.of(DrawStatus.SETTLED),
            DrawStatus.SETTLED, Set.of(DrawStatus.ARCHIVED),
            DrawStatus.CANCELED, Set.of(DrawStatus.ARCHIVED),
            DrawStatus.ARCHIVED, Set.of());

    @Nested
    @DisplayName("full from×to matrix")
    class FullMatrix {

        @Test
        @DisplayName("canTransition matches the expected legal matrix for every pair")
        void canTransitionMatchesMatrix() {
            for (DrawStatus from : DrawStatus.values()) {
                Set<DrawStatus> legal = EXPECTED_LEGAL.getOrDefault(from, Set.of());
                for (DrawStatus to : DrawStatus.values()) {
                    boolean expected = legal.contains(to);
                    assertThat(DrawStatusTransition.canTransition(from, to))
                        .as("canTransition(%s -> %s) should be %s", from, to, expected)
                        .isEqualTo(expected);
                }
            }
        }

        @Test
        @DisplayName("check() passes for every legal pair and throws for every illegal pair")
        void checkMatchesMatrix() {
            for (DrawStatus from : DrawStatus.values()) {
                Set<DrawStatus> legal = EXPECTED_LEGAL.getOrDefault(from, Set.of());
                for (DrawStatus to : DrawStatus.values()) {
                    if (legal.contains(to)) {
                        assertThatCode(() -> DrawStatusTransition.check(from, to))
                            .as("check(%s -> %s) should pass", from, to)
                            .doesNotThrowAnyException();
                    } else {
                        assertThatThrownBy(() -> DrawStatusTransition.check(from, to))
                            .as("check(%s -> %s) should throw", from, to)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Invalid draw status transition")
                            .hasMessageContaining(from.name())
                            .hasMessageContaining(to.name());
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("spot-checks on business-meaningful edges")
    class SpotChecks {

        @Test
        @DisplayName("forward lifecycle edges are legal")
        void forwardEdgesLegal() {
            assertThat(DrawStatusTransition.canTransition(DrawStatus.SCHEDULED, DrawStatus.OPEN)).isTrue();
            assertThat(DrawStatusTransition.canTransition(DrawStatus.OPEN, DrawStatus.CLOSED)).isTrue();
            assertThat(DrawStatusTransition.canTransition(DrawStatus.CLOSED, DrawStatus.RESULTED)).isTrue();
            assertThat(DrawStatusTransition.canTransition(DrawStatus.RESULTED, DrawStatus.SETTLED)).isTrue();
            assertThat(DrawStatusTransition.canTransition(DrawStatus.SETTLED, DrawStatus.ARCHIVED)).isTrue();
        }

        @Test
        @DisplayName("you cannot skip a rung, go backwards, cancel after RESULTED, or leave a terminal state")
        void illegalEdgesRejected() {
            // skip a rung
            assertThat(DrawStatusTransition.canTransition(DrawStatus.OPEN, DrawStatus.RESULTED)).isFalse();
            assertThat(DrawStatusTransition.canTransition(DrawStatus.OPEN, DrawStatus.SETTLED)).isFalse();
            // backwards
            assertThat(DrawStatusTransition.canTransition(DrawStatus.CLOSED, DrawStatus.OPEN)).isFalse();
            assertThat(DrawStatusTransition.canTransition(DrawStatus.SETTLED, DrawStatus.RESULTED)).isFalse();
            // cancel is only allowed up to CLOSED, never after results exist
            assertThat(DrawStatusTransition.canTransition(DrawStatus.RESULTED, DrawStatus.CANCELED)).isFalse();
            // terminal states have no outgoing edge
            assertThat(DrawStatusTransition.canTransition(DrawStatus.ARCHIVED, DrawStatus.SCHEDULED)).isFalse();
        }

        @Test
        @DisplayName("a status never transitions to itself")
        void noSelfTransition() {
            for (DrawStatus s : DrawStatus.values()) {
                assertThat(DrawStatusTransition.canTransition(s, s))
                    .as("%s -> %s (self) should be illegal", s, s)
                    .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("null handling")
    class NullHandling {

        @Test
        @DisplayName("null from or to is rejected with NPE on both check() and canTransition()")
        void nullRejected() {
            assertThatNullPointerException()
                .isThrownBy(() -> DrawStatusTransition.check(null, DrawStatus.OPEN));
            assertThatNullPointerException()
                .isThrownBy(() -> DrawStatusTransition.check(DrawStatus.OPEN, null));
            assertThatNullPointerException()
                .isThrownBy(() -> DrawStatusTransition.canTransition(null, DrawStatus.OPEN));
            assertThatNullPointerException()
                .isThrownBy(() -> DrawStatusTransition.canTransition(DrawStatus.OPEN, null));
        }
    }
}
