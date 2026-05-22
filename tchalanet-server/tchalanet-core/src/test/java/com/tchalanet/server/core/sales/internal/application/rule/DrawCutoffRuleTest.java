package com.tchalanet.server.core.sales.internal.application.rule;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DrawCutoffRule")
class DrawCutoffRuleTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
    private static final DrawChannelId CHANNEL = DrawChannelId.of(UUID.randomUUID());
    private static final DrawId DRAW = DrawId.of(UUID.randomUUID());

    private static final Instant CUTOFF = Instant.parse("2026-05-20T14:00:00Z");
    private static final Instant SCHEDULED = Instant.parse("2026-05-20T14:05:00Z");

    /** Builds a rule with a fixed clock set to the given instant. */
    private DrawCutoffRule ruleAt(Instant now) {
        var clock = Clock.fixed(now, UTC);
        var timeProvider = new TimeProvider(clock);
        QueryBus queryBus = new QueryBus() {
            @Override
            @SuppressWarnings("unchecked")
            public <R> R ask(com.tchalanet.server.common.bus.Query<R> query) {
                if (query instanceof GetDrawByIdQuery q && q.id().equals(DRAW)) {
                    return (R) makeOpenDraw();
                }
                throw new UnsupportedOperationException("unexpected query: " + query);
            }
        };
        return new DrawCutoffRule(queryBus, timeProvider);
    }

    private DrawSummary makeOpenDraw() {
        return new DrawSummary(
            DRAW, TENANT, LocalDate.of(2026, 5, 20), DrawStatus.OPEN,
            SCHEDULED, Instant.parse("2026-05-20T08:00:00Z"),
            null, CUTOFF, null, null,
            CHANNEL, "MID", "Midi", null, "America/Port-au-Prince", true,
            null, null, null, null, null, false,
            null);
    }

    @Nested
    @DisplayName("Sale before cutoff")
    class BeforeCutoff {

        @Test
        @DisplayName("1 second before cutoff — allowed")
        void oneSecondBefore() {
            var rule = ruleAt(CUTOFF.minusSeconds(1));
            DrawSummary result = rule.requireBeforeCutoff(DRAW);
            assertThat(result.drawId()).isEqualTo(DRAW);
        }

        @Test
        @DisplayName("1 minute before cutoff — allowed")
        void oneMinuteBefore() {
            var rule = ruleAt(CUTOFF.minusSeconds(60));
            assertThat(rule.requireBeforeCutoff(DRAW)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Sale at or after cutoff")
    class AtOrAfterCutoff {

        @Test
        @DisplayName("exactly at cutoff — rejected")
        void exactlyAtCutoff() {
            var rule = ruleAt(CUTOFF);
            assertThatThrownBy(() -> rule.requireBeforeCutoff(DRAW))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("1 second after cutoff — rejected")
        void oneSecondAfter() {
            var rule = ruleAt(CUTOFF.plusSeconds(1));
            assertThatThrownBy(() -> rule.requireBeforeCutoff(DRAW))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("well after cutoff — rejected")
        void wellAfterCutoff() {
            var rule = ruleAt(CUTOFF.plusSeconds(3600));
            assertThatThrownBy(() -> rule.requireBeforeCutoff(DRAW))
                .isInstanceOf(ProblemRestException.class);
        }
    }
}
