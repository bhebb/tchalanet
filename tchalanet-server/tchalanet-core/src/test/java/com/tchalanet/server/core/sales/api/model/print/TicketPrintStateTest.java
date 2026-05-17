package com.tchalanet.server.core.sales.api.model.print;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TicketPrintState")
class TicketPrintStateTest {

    @Nested
    @DisplayName("markPrinted")
    class MarkPrinted {

        @Test
        @DisplayName("should transition NOT_PRINTED to PRINTED on first print")
        void shouldTransitionFromNotPrintedToPrinted() {
            var now = Instant.parse("2026-05-16T10:00:00Z");

            var state = TicketPrintState.notPrinted().markPrinted(now);

            assertThat(state.status()).isEqualTo(TicketPrintStateStatus.PRINTED);
            assertThat(state.printCount()).isEqualTo(1);
            assertThat(state.firstPrintedAt()).isEqualTo(now);
            assertThat(state.lastPrintedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should transition PRINTED to REPRINTED and preserve firstPrintedAt")
        void shouldTransitionPrintedToReprinted() {
            var first = Instant.parse("2026-05-16T10:00:00Z");
            var second = Instant.parse("2026-05-16T10:05:00Z");

            var state = TicketPrintState.notPrinted().markPrinted(first).markPrinted(second);

            assertThat(state.status()).isEqualTo(TicketPrintStateStatus.REPRINTED);
            assertThat(state.printCount()).isEqualTo(2);
            assertThat(state.firstPrintedAt()).isEqualTo(first);
            assertThat(state.lastPrintedAt()).isEqualTo(second);
        }

        @Test
        @DisplayName("should keep REPRINTED and increment count on next reprint")
        void shouldKeepReprintedOnSubsequentReprints() {
            var first = Instant.parse("2026-05-16T10:00:00Z");
            var second = Instant.parse("2026-05-16T10:05:00Z");
            var third = Instant.parse("2026-05-16T10:10:00Z");

            var state = TicketPrintState.notPrinted()
                .markPrinted(first)
                .markPrinted(second)
                .markPrinted(third);

            assertThat(state.status()).isEqualTo(TicketPrintStateStatus.REPRINTED);
            assertThat(state.printCount()).isEqualTo(3);
            assertThat(state.firstPrintedAt()).isEqualTo(first);
            assertThat(state.lastPrintedAt()).isEqualTo(third);
        }
    }
}

