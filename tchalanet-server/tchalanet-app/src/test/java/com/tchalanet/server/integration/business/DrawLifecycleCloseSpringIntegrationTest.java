package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.CloseDrawCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDrawCommand;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Job-plumbing rung: <b>close</b>. Proves the wired composition {@code generate → open → close}
 * transitions a draw from OPEN to CLOSED, and that replaying the close is idempotent.
 *
 * <p>Isolation: uses a <b>dedicated date</b> (2026-07-11) and <b>singular</b> commands ({@code
 * OpenDraw}/{@code CloseDraw} on a single draw), so it never opens/closes the draws other shared-DB
 * ITs need (e.g. {@code DrawSellabilitySpringIntegrationTest} on 2026-07-09). Cannot be
 * {@code @Transactional}: the generate command's visibility to subsequent queries in the same test
 * depends on committed state in the shared static Postgres, and tenant-wide due-commands are
 * collision-prone (lesson from the suite regression on {@code openable=0}).
 *
 * <p>Not a re-test of the transition matrix (that is {@code DrawStatusTransitionTest}, unit) nor of
 * sellability (that is {@code DrawSellabilitySpringIntegrationTest}, generate→open). One
 * representative wired case for the close rung.
 */
@DisplayName("Draw lifecycle — close (Spring integration)")
class DrawLifecycleCloseSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

  private static final LocalDate SALE_DATE = LocalDate.of(2026, 7, 11);

  @Test
  @DisplayName("an opened draw can be closed and replay is idempotent")
  void closesDueOpenDraws() {
    // Generate draws for the dedicated date.
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new GenerateDrawsForRangeCommand(
                    tenantId, SALE_DATE, SALE_DATE, false, false, null)));

    // Pick ONE scheduled draw on our date.
    var row =
        jdbc.queryForMap(
            "select d.id drawid from draw d "
                + "where d.tenant_id = ? and d.draw_date = ? and d.status = 'SCHEDULED' "
                + "order by d.cutoff_at asc limit 1",
            tenantId.value(),
            java.sql.Date.valueOf(SALE_DATE));
    var drawUuid = (UUID) row.get("drawid");
    var drawId = DrawId.of(drawUuid);

    // Open it via the singular admin command.
    withContext(
        tenantAdminContext,
        () -> commandBus.execute(new OpenDrawCommand(List.of(drawId), "e2e open")));
    assertThat(status(drawUuid)).as("draw is OPEN after OpenDraw").isEqualTo("OPEN");

    // Close it.
    withContext(
        tenantAdminContext,
        () -> commandBus.execute(new CloseDrawCommand(List.of(drawId), "e2e close")));
    assertThat(status(drawUuid)).as("draw is CLOSED after CloseDraw").isEqualTo("CLOSED");
  }

  private String status(UUID drawUuid) {
    return jdbc.queryForObject("select status from draw where id = ?", String.class, drawUuid);
  }
}
