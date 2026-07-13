package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.SettleDrawCommand;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.drawresult.api.command.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end draw result lifecycle over the real stack:
 * generate → open → close → record+apply (CLOSED→RESULTED) → override → settle.
 *
 * <p>Guards the override fix: overriding an already-RESULTED draw's result must NOT
 * throw. Before the fix, {@code applyOverrideToDraws} called {@code draw.applyResult}
 * on a RESULTED draw and threw {@code IllegalStateException: RESULTED -> RESULTED}
 * (observed end-to-end). It now uses {@code draw.overrideResult}.
 */
@DisplayName("Draw result override + settle (Spring integration)")
class DrawResultOverrideSettleSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

    private static final LocalDate SALE_DATE = LocalDate.of(2026, 7, 9);
    private static final Instant AFTER_CUTOFF = Instant.parse("2026-07-10T00:00:00Z");

    @Test
    @DisplayName("a resulted draw can be overridden without throwing, then settled")
    void overrideResultedDrawThenSettle() {
        withContext(tenantAdminContext, () -> commandBus.execute(
            new GenerateDrawsForRangeCommand(tenantId, SALE_DATE, SALE_DATE, false, false, null)));
        withContext(tenantAdminContext, () -> commandBus.execute(
            new OpenDueDrawsCommand(FIXED_NOW, 100, 48, 1, false)));
        withContext(tenantAdminContext, () -> commandBus.execute(
            new CloseDueDrawsCommand(AFTER_CUTOFF, 100, false)));

        // Pick one closed draw and its slot.
        Map<String, Object> row = jdbc.queryForList(
            "select d.id drawid, d.draw_date ddate, rs.slot_key skey "
                + "from draw d join draw_channel dc on dc.id = d.draw_channel_id "
                + "join result_slot rs on rs.id = dc.result_slot_id "
                + "where d.tenant_id = ? and d.status = 'CLOSED' order by d.cutoff_at asc limit 1",
            tenantId.value()).get(0);
        var drawUuid = (UUID) row.get("drawid");
        var drawId = DrawId.of(drawUuid);
        var drawDate = ((java.sql.Date) row.get("ddate")).toLocalDate();
        var slotKey = (String) row.get("skey");

        // Record a manual result, then apply the window → draw becomes RESULTED.
        withContext(tenantAdminContext, () -> commandBus.execute(new RecordManualDrawResultCommand(
            tenantId, drawDate, slotKey, "e2e", null, "123", "1234", false, null, false)));
        withContext(tenantAdminContext, () -> commandBus.execute(new ApplyExternalResultsWindowCommand(
            tenantId, drawDate, 1, List.of(slotKey), false, false, 50, "e2e apply")));

        assertThat(status(drawUuid)).as("apply transitions CLOSED→RESULTED").isEqualTo("RESULTED");
        assertThat(drawResultId(drawUuid)).as("a result is attached").isNotNull();

        // The fix under test: overriding a RESULTED draw must not throw. This runs
        // applyOverrideToDraws → draw.overrideResult on the RESULTED draw. Before the
        // fix (applyResult) this threw IllegalStateException end-to-end (RED proven by
        // temporarily reverting). force=false keeps the CONFIRMED result settleable.
        assertThatCode(() -> withContext(tenantAdminContext, () -> commandBus.execute(
            new OverrideDrawResultCommand(tenantId, slotKey, drawDate, "456", "5678", "e2e override reason", false))))
            .doesNotThrowAnyException();
        assertThat(status(drawUuid)).as("draw stays RESULTED after override").isEqualTo("RESULTED");

        // And it can then be settled.
        withContext(tenantAdminContext, () -> commandBus.execute(
            new SettleDrawCommand(List.of(drawId), "e2e settle", false)));
        assertThat(status(drawUuid)).as("RESULTED→SETTLED").isEqualTo("SETTLED");
        var settledAt = settledAt(drawUuid);
        assertThat(settledAt).isNotNull();

        // Money safety: replaying settle is an idempotent no-op — it does not throw,
        // does not re-settle, and produces no duplicate money effect (settled_at
        // unchanged). Makes the settle job retry-safe.
        assertThatCode(() -> withContext(tenantAdminContext, () -> commandBus.execute(
            new SettleDrawCommand(List.of(drawId), "e2e settle replay", false))))
            .doesNotThrowAnyException();
        assertThat(status(drawUuid)).as("still SETTLED after replay").isEqualTo("SETTLED");
        assertThat(settledAt(drawUuid)).as("settled_at unchanged — not re-settled").isEqualTo(settledAt);
    }

    private String status(UUID drawUuid) {
        return jdbc.queryForObject("select status from draw where id = ?", String.class, drawUuid);
    }

    private UUID drawResultId(UUID drawUuid) {
        return jdbc.queryForObject("select draw_result_id from draw where id = ?", UUID.class, drawUuid);
    }

    private java.sql.Timestamp settledAt(UUID drawUuid) {
        return jdbc.queryForObject("select settled_at from draw where id = ?", java.sql.Timestamp.class, drawUuid);
    }
}
