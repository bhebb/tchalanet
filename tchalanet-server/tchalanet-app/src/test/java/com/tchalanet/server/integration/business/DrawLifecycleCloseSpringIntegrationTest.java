package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.draw.api.command.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Job-plumbing rung: <b>close</b>. Proves the wired composition
 * {@code generate → open → close} advances OPEN draws past their cutoff to CLOSED
 * once, is idempotent on replay, and that {@code dryRun} does not persist.
 *
 * <p>Not a re-test of the transition matrix (that is
 * {@code DrawStatusTransitionTest}, unit) nor of sellability (that is
 * {@code DrawSellabilitySpringIntegrationTest}, generate→open). One representative
 * wired case for the close rung.
 */
@DisplayName("Draw lifecycle — close due draws (Spring integration)")
class DrawLifecycleCloseSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

    // Distinct date from the other draw-lifecycle ITs: the base shares one static
    // Postgres with no per-class cleanup, so working the same date collides (a prior
    // IT would already have closed those draws). 2026-07-10 is still inside the 48h
    // open window from FIXED_NOW (2026-07-08T14:00Z → 2026-07-10T14:00Z).
    private static final LocalDate SALE_DATE = LocalDate.of(2026, 7, 10);
    /** Safely after the cutoff of any 2026-07-10 draw. */
    private static final Instant AFTER_CUTOFF = Instant.parse("2026-07-11T00:00:00Z");

    @Test
    @DisplayName("open draws past cutoff are closed once; dryRun is a no-op; replay closes nothing more")
    void closesDueOpenDraws() {
        withContext(tenantAdminContext, () ->
            commandBus.execute(new GenerateDrawsForRangeCommand(
                tenantId, SALE_DATE, SALE_DATE, false, false, null)));
        withContext(tenantAdminContext, () ->
            commandBus.execute(new OpenDueDrawsCommand(FIXED_NOW, 100, 48, 1, false)));

        // Counts are read from v_draw_summary (aggregated view) — directional
        // invariants only, never equated to command result counts, which are at
        // draw-entity granularity (a known, different granularity).
        int openBefore = statusCount("OPEN");
        int closedBefore = statusCount("CLOSED");
        assertThat(openBefore).as("draws were opened").isGreaterThan(0);

        // dryRun MUST NOT persist any state change.
        withContext(tenantAdminContext, () ->
            commandBus.execute(new CloseDueDrawsCommand(AFTER_CUTOFF, 100, true)));
        assertThat(statusCount("OPEN")).as("dryRun leaves OPEN untouched").isEqualTo(openBefore);
        assertThat(statusCount("CLOSED")).as("dryRun creates no CLOSED").isEqualTo(closedBefore);

        // Real close: due OPEN draws move to CLOSED.
        var closed = withContext(tenantAdminContext, () ->
            commandBus.execute(new CloseDueDrawsCommand(AFTER_CUTOFF, 100, false)));
        assertThat(closed.closed()).as("close reports draws closed").isGreaterThan(0);
        int openAfter = statusCount("OPEN");
        int closedAfter = statusCount("CLOSED");
        assertThat(openAfter).as("fewer OPEN after close").isLessThan(openBefore);
        assertThat(closedAfter).as("more CLOSED after close").isGreaterThan(closedBefore);

        // Idempotent: replaying the same 'now' closes nothing more, persists nothing.
        var again = withContext(tenantAdminContext, () ->
            commandBus.execute(new CloseDueDrawsCommand(AFTER_CUTOFF, 100, false)));
        assertThat(again.closed()).as("replay is idempotent").isZero();
        assertThat(statusCount("OPEN")).isEqualTo(openAfter);
        assertThat(statusCount("CLOSED")).isEqualTo(closedAfter);
    }

    private int statusCount(String status) {
        return jdbc.queryForObject(
            "select count(*) from v_draw_summary where tenant_id = ? and status = ?",
            Integer.class,
            tenantId.value(),
            status);
    }
}
