package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.command.CloseDrawCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDrawCommand;
import com.tchalanet.server.core.draw.internal.application.service.ResultedDrawProcessor;
import com.tchalanet.server.core.drawresult.api.command.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Draw + ticket lifecycle (Spring integration)")
class DrawTicketLifecycleSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final LocalDate DRAW_DATE = LocalDate.of(2026, 7, 23);
  private static final String SLOT_KEY = "NY_MID";

  @Autowired private ResultedDrawProcessor resultedDrawProcessor;

  @Test
  @DisplayName("sell ticket lines, apply bad result, override it, then settle draw")
  void sellApplyBadResultOverrideTicketLinesAndSettleDraw() {
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new GenerateDrawsForRangeCommand(
                    tenantId, DRAW_DATE, DRAW_DATE, false, false, null)));

    var row = scheduledBoletDraw(SLOT_KEY, DRAW_DATE);
    var drawUuid = (UUID) row.get("draw_id");
    var drawId = DrawId.of(drawUuid);
    var drawChannelId =
        com.tchalanet.server.common.types.id.DrawChannelId.of((UUID) row.get("draw_channel_id"));

    withContext(
        tenantAdminContext,
        () -> commandBus.execute(new OpenDrawCommand(List.of(drawId), "it ticket lifecycle open")));

    var sold =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new SellTicketCommand(
                        drawId,
                        drawChannelId,
                        HTG,
                        List.of(
                            new SellTicketLineInput(
                                1,
                                GameCode.HT_BOLET,
                                BetType.MATCH_1_2D,
                                "23",
                                null,
                                new BigDecimal("10")),
                            new SellTicketLineInput(
                                2,
                                GameCode.HT_BOLET,
                                BetType.MATCH_1_2D,
                                "99",
                                null,
                                new BigDecimal("10"))),
                        SaleCommunicationOptions.none(),
                        List.of())));

    assertThat(sold.outcome()).isEqualTo(SellTicketOutcome.ACCEPTED);
    var ticketId = sold.ticketId().value();
    assertTicketBeforeResult(ticketId);

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(new CloseDrawCommand(List.of(drawId), "it ticket lifecycle close")));
    assertThat(drawStatus(drawUuid)).isEqualTo("CLOSED");

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new RecordManualDrawResultCommand(
                    tenantId,
                    DRAW_DATE,
                    SLOT_KEY,
                    "it-ticket-lifecycle",
                    "ticket lifecycle result",
                    "123",
                    "4567",
                    false,
                    null,
                    false)));

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new ApplyExternalResultsWindowCommand(
                    tenantId,
                    DRAW_DATE,
                    0,
                    List.of(SLOT_KEY),
                    false,
                    false,
                    1,
                    "it ticket lifecycle apply")));

    assertThat(drawStatus(drawUuid)).isEqualTo("RESULTED");
    assertThat(drawResultId(drawUuid)).isNotNull();
    assertTicketAfterResult(ticketId);
    assertTicketLinesAfterResult(ticketId);

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new OverrideDrawResultCommand(
                    tenantId,
                    SLOT_KEY,
                    DRAW_DATE,
                    "111",
                    "2222",
                    "superadmin correction after tenant entered wrong result",
                    true)));

    assertThat(drawStatus(drawUuid)).isEqualTo("RESULTED");
    assertThat(drawResultSource(drawUuid)).isEqualTo("ADMIN_OVERRIDE");
    assertTicketAfterOverrideLoses(ticketId);
    assertTicketLinesAfterOverrideLoses(ticketId);

    var processed = withContext(tenantAdminContext, () -> resultedDrawProcessor.process(drawId));

    assertThat(processed).isTrue();
    assertThat(drawStatus(drawUuid)).isEqualTo("SETTLED");
    assertTicketAfterOverrideLoses(ticketId);
    assertTicketLinesAfterOverrideLoses(ticketId);
  }

  private Map<String, Object> scheduledBoletDraw(String slotKey, LocalDate drawDate) {
    return jdbc.queryForList(
            """
                select d.id as draw_id, d.draw_channel_id
                  from draw d
                  join draw_channel dc on dc.id = d.draw_channel_id
                  join result_slot rs on rs.id = dc.result_slot_id
                  join draw_channel_game dcg on dcg.draw_channel_id = dc.id
                  join tenant_game tg on tg.id = dcg.tenant_game_id
                  join game g on g.id = tg.game_id
                 where d.tenant_id = ?
                   and d.draw_date = ?
                   and d.status = 'SCHEDULED'
                   and rs.slot_key = ?
                   and g.code = 'HT_BOLET'
                   and dcg.enabled = true
                 order by d.cutoff_at asc
                 limit 1
                """,
            tenantId.value(),
            java.sql.Date.valueOf(drawDate),
            slotKey)
        .getFirst();
  }

  private void assertTicketBeforeResult(UUID ticketId) {
    var ticket =
        jdbc.queryForMap(
            """
                select result_status, settlement_status, winning_amount
                  from sales_ticket
                 where id = ?
                """,
            ticketId);

    assertThat(ticket.get("result_status")).isEqualTo("NOT_RESULTED");
    assertThat(ticket.get("settlement_status")).isEqualTo("NOT_SETTLED");
    assertThat((BigDecimal) ticket.get("winning_amount")).isEqualByComparingTo("0");

    assertThat(
            jdbc.queryForObject(
                "select count(*) from sales_ticket_line where ticket_id = ?",
                Integer.class,
                ticketId))
        .isEqualTo(2);
  }

  private void assertTicketAfterResult(UUID ticketId) {
    var ticket =
        jdbc.queryForMap(
            """
                select result_status, settlement_status, winning_amount, resulted_at, settled_at
                  from sales_ticket
                 where id = ?
                """,
            ticketId);

    assertThat(ticket.get("result_status")).isEqualTo("WON");
    assertThat(ticket.get("settlement_status")).isEqualTo("PAID");
    assertThat((BigDecimal) ticket.get("winning_amount")).isGreaterThan(BigDecimal.ZERO);
    assertThat(ticket.get("resulted_at")).isNotNull();
    assertThat(ticket.get("settled_at")).isNotNull();
  }

  private void assertTicketLinesAfterResult(UUID ticketId) {
    var lines =
        jdbc.queryForList(
            """
                select line_number, selection_key, result_status, payout_amount
                  from sales_ticket_line
                 where ticket_id = ?
                 order by line_number
                """,
            ticketId);

    assertThat(lines).hasSize(2);
    assertThat(lines.get(0).get("line_number")).isEqualTo(1);
    assertThat(lines.get(0).get("selection_key")).isEqualTo("23");
    assertThat(lines.get(0).get("result_status")).isEqualTo("WON");
    assertThat((BigDecimal) lines.get(0).get("payout_amount")).isGreaterThan(BigDecimal.ZERO);

    assertThat(lines.get(1).get("line_number")).isEqualTo(2);
    assertThat(lines.get(1).get("selection_key")).isEqualTo("99");
    assertThat(lines.get(1).get("result_status")).isEqualTo("LOST");
    assertThat((BigDecimal) lines.get(1).get("payout_amount")).isEqualByComparingTo("0");
  }

  private void assertTicketAfterOverrideLoses(UUID ticketId) {
    var ticket =
        jdbc.queryForMap(
            """
                select result_status, settlement_status, winning_amount, paid_amount, result_override_reason
                  from sales_ticket
                 where id = ?
                """,
            ticketId);

    assertThat(ticket.get("result_status")).isEqualTo("OVERRIDDEN");
    assertThat(ticket.get("settlement_status")).isEqualTo("NO_PAYOUT");
    assertThat((BigDecimal) ticket.get("winning_amount")).isEqualByComparingTo("0");
    assertThat((BigDecimal) ticket.get("paid_amount")).isEqualByComparingTo("0");
    assertThat(ticket.get("result_override_reason"))
        .isEqualTo("superadmin correction after tenant entered wrong result");
  }

  private void assertTicketLinesAfterOverrideLoses(UUID ticketId) {
    var lines =
        jdbc.queryForList(
            """
                select line_number, selection_key, result_status, payout_amount
                  from sales_ticket_line
                 where ticket_id = ?
                 order by line_number
                """,
            ticketId);

    assertThat(lines).hasSize(2);
    assertThat(lines.get(0).get("selection_key")).isEqualTo("23");
    assertThat(lines.get(0).get("result_status")).isEqualTo("LOST");
    assertThat((BigDecimal) lines.get(0).get("payout_amount")).isEqualByComparingTo("0");

    assertThat(lines.get(1).get("selection_key")).isEqualTo("99");
    assertThat(lines.get(1).get("result_status")).isEqualTo("LOST");
    assertThat((BigDecimal) lines.get(1).get("payout_amount")).isEqualByComparingTo("0");
  }

  private String drawStatus(UUID drawUuid) {
    return jdbc.queryForObject("select status from draw where id = ?", String.class, drawUuid);
  }

  private UUID drawResultId(UUID drawUuid) {
    return jdbc.queryForObject(
        "select draw_result_id from draw where id = ?", UUID.class, drawUuid);
  }

  private String drawResultSource(UUID drawUuid) {
    return jdbc.queryForObject(
        "select result_source from draw where id = ?", String.class, drawUuid);
  }
}
