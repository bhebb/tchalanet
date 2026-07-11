package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.features.pos.draws.PosDrawsService;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Draw sellability Spring integration")
class DrawSellabilitySpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

    @Autowired
    private PosDrawsService posDrawsService;

    @Test
    @DisplayName("should expose generated and opened draws to cashier POS")
    void shouldExposeGeneratedAndOpenedDrawsToCashierPos() {
        var saleDate = LocalDate.of(2026, 7, 9);

        var generated = withContext(tenantAdminContext, () ->
            commandBus.execute(new GenerateDrawsForRangeCommand(
                tenantId,
                saleDate,
                saleDate,
                false,
                false,
                null)));
        var opened = withContext(tenantAdminContext, () ->
            commandBus.execute(new OpenDueDrawsCommand(FIXED_NOW, 100, 48, 1, false)));

        var openDrawCount = jdbc.queryForObject(
            """
            select count(*)
            from v_draw_summary
            where tenant_id = ?
              and status = 'OPEN'
              and cutoff_at > ?
              and scheduled_at < ?
            """,
            Integer.class,
            tenantId.value(),
            Timestamp.from(FIXED_NOW),
            Timestamp.from(FIXED_NOW.plusSeconds(48 * 3600)));
        var cashierRows = withContext(sellerContext, () ->
            queryBus.ask(new ListCashierNextDrawsQuery(48, 20)));
        var available = withContext(sellerContext, () ->
            posDrawsService.listAvailable(sellerContext, 48, 20));

        assertThat(generated.created()).isGreaterThan(0);
        assertThat(opened.opened()).isGreaterThan(0);
        assertThat(openDrawCount).isGreaterThan(0);
        assertThat(cashierRows).isNotEmpty();
        assertThat(available)
            .isNotEmpty()
            .allSatisfy(draw -> {
                assertThat(draw.status()).isEqualTo("OPEN");
                assertThat(draw.drawId()).isNotNull();
                assertThat(draw.drawChannelId()).isNotNull();
                assertThat(draw.gameCodes()).isNotEmpty();
            });
    }
}
