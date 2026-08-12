package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.features.pos.draws.PosAvailableDrawView;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Limit policy runtime scenarios Spring integration")
class LimitPolicyRuntimeIntegrationTest extends BusinessRuntimeIntegrationTestBase {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final ZoneId TENANT_ZONE = ZoneId.of("America/Port-au-Prince");
  private static final Instant SALES_NOW = Instant.now();

  @Test
  @DisplayName("Cas 1 — block selection rejects sale and does not persist a ticket")
  void blockSelectionShouldRejectWithNoTicket() throws Exception {
    var draw = openedDrawContaining(GameCode.HT_BOLET);
    var before = countTickets();

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertLimitAssignmentCommand(
                    tenantId,
                    RuleKey.BLOCK_SELECTION_PER_DRAW,
                    LimitScopeRef.tenant(tenantId),
                    true,
                    BreachOutcome.BLOCK,
                    JSON.readTree(
                        """
                        {"betType":"MATCH_1_2D","selections":["34"]}
                        """),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400))));

    var result =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new SellTicketCommand(
                        draw.drawId(),
                        draw.drawChannelId(),
                        HTG,
                        List.of(
                            new SellTicketLineInput(
                                1,
                                GameCode.HT_BOLET,
                                BetType.MATCH_1_2D,
                                "34",
                                null,
                                new BigDecimal("10"))),
                        SaleCommunicationOptions.none(),
                        List.of())));

    assertThat(result.outcome()).isEqualTo(SellTicketOutcome.REJECTED);
    assertThat(countTickets()).isEqualTo(before);
  }

  @Test
  @DisplayName(
      "Cas 2 — SELLER_TERMINAL overrides DRAW_CHANNEL overrides TENANT for MAX_STAKE_PER_LINE")
  void mostSpecificScopeWins() throws Exception {
    var draw = openedDrawContaining(GameCode.HT_BOLET);
    var sellerTerminalId = sellerContext.sellerTerminalIdRequired();

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertLimitAssignmentCommand(
                    tenantId,
                    RuleKey.MAX_STAKE_PER_LINE,
                    LimitScopeRef.tenant(tenantId),
                    true,
                    BreachOutcome.BLOCK,
                    JSON.readTree(
                        """
                        {"valueCents":50000}
                        """),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400))));
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertLimitAssignmentCommand(
                    tenantId,
                    RuleKey.MAX_STAKE_PER_LINE,
                    LimitScopeRef.drawChannel(draw.drawChannelId()),
                    true,
                    BreachOutcome.BLOCK,
                    JSON.readTree(
                        """
                        {"valueCents":30000}
                        """),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400))));
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertLimitAssignmentCommand(
                    tenantId,
                    RuleKey.MAX_STAKE_PER_LINE,
                    LimitScopeRef.sellerTerminal(sellerTerminalId),
                    true,
                    BreachOutcome.BLOCK,
                    JSON.readTree(
                        """
                        {"valueCents":10000}
                        """),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400))));

    // 150 HTG = 15000 cents > SELLER_TERMINAL limit 10000 cents → REJECTED
    var rejected =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new SellTicketCommand(
                        draw.drawId(),
                        draw.drawChannelId(),
                        HTG,
                        List.of(
                            new SellTicketLineInput(
                                1,
                                GameCode.HT_BOLET,
                                BetType.MATCH_1_2D,
                                "07",
                                null,
                                new BigDecimal("150"))),
                        SaleCommunicationOptions.none(),
                        List.of())));

    assertThat(rejected.outcome()).isEqualTo(SellTicketOutcome.REJECTED);

    // 50 HTG = 5000 cents < SELLER_TERMINAL limit 10000 cents → ACCEPTED
    var accepted =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new SellTicketCommand(
                        draw.drawId(),
                        draw.drawChannelId(),
                        HTG,
                        List.of(
                            new SellTicketLineInput(
                                1,
                                GameCode.HT_BOLET,
                                BetType.MATCH_1_2D,
                                "07",
                                null,
                                new BigDecimal("50"))),
                        SaleCommunicationOptions.none(),
                        List.of())));

    assertThat(accepted.outcome()).isEqualTo(SellTicketOutcome.ACCEPTED);
  }

  @Test
  @DisplayName("Cas 3 — cumulative exposure stops at limit without counting rejected sale")
  void cumulativeExposureStopsAtLimit() throws Exception {
    var draw = openedDrawContaining(GameCode.HT_BOLET);

    // limit = 1000 HTG = 100000 cents at DRAW_CHANNEL scope for selection "12"
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertLimitAssignmentCommand(
                    tenantId,
                    RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
                    LimitScopeRef.drawChannel(draw.drawChannelId()),
                    true,
                    BreachOutcome.BLOCK,
                    JSON.readTree(
                        """
                        {"valueCents":100000}
                        """),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400))));

    // 9 × 100 HTG on "12" → ACCEPTED (cumul = 900 HTG)
    for (int i = 0; i < 9; i++) {
      var result =
          withContext(
              sellerContext,
              () ->
                  commandBus.execute(
                      new SellTicketCommand(
                          draw.drawId(),
                          draw.drawChannelId(),
                          HTG,
                          List.of(
                              new SellTicketLineInput(
                                  1,
                                  GameCode.HT_BOLET,
                                  BetType.MATCH_1_2D,
                                  "12",
                                  null,
                                  new BigDecimal("100"))),
                          SaleCommunicationOptions.none(),
                          List.of())));
      assertThat(result.outcome()).isEqualTo(SellTicketOutcome.ACCEPTED);
    }

    // 1 × 200 HTG → REJECTED (900 + 200 = 1100 HTG > 1000 HTG limit)
    var blocked =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new SellTicketCommand(
                        draw.drawId(),
                        draw.drawChannelId(),
                        HTG,
                        List.of(
                            new SellTicketLineInput(
                                1,
                                GameCode.HT_BOLET,
                                BetType.MATCH_1_2D,
                                "12",
                                null,
                                new BigDecimal("200"))),
                        SaleCommunicationOptions.none(),
                        List.of())));

    assertThat(blocked.outcome()).isEqualTo(SellTicketOutcome.REJECTED);

    // Counter stays at 900 HTG (rejected sale not counted)
    var stakeTotal =
        jdbc.queryForObject(
            """
                SELECT stake_total FROM draw_exposure
                WHERE draw_id = ?
                  AND scope_type = 'DRAW_CHANNEL'
                  AND scope_id = ?
                  AND bet_type = 'MATCH_1_2D'
                  AND selection_key = '12'
                """,
            BigDecimal.class,
            draw.drawId().value(),
            draw.drawChannelId().value());

    assertThat(stakeTotal).isEqualByComparingTo(new BigDecimal("900.00"));
  }

  private PosAvailableDrawView openedDrawContaining(GameCode gameCode) {
    var saleDate = LocalDate.now(TENANT_ZONE).plusDays(1);
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new GenerateDrawsForRangeCommand(
                    tenantId, saleDate, saleDate, false, false, null)));
    withContext(
        tenantAdminContext,
        () -> commandBus.execute(new OpenDueDrawsCommand(SALES_NOW, 100, 48, 1, false)));

    return jdbc.query(
        """
            select d.id as draw_id, d.draw_channel_id, d.scheduled_at, d.cutoff_at
            from draw d
            join draw_channel_game dcg
              on dcg.draw_channel_id = d.draw_channel_id
             and dcg.deleted_at is null
             and dcg.enabled = true
            join tenant_game tg
              on tg.id = dcg.tenant_game_id
             and tg.deleted_at is null
             and tg.enabled = true
            where d.tenant_id = ?
              and d.status = 'OPEN'
              and d.cutoff_at > ?
              and tg.game_code = ?
            order by d.scheduled_at
            limit 1
            """,
        rs -> {
          if (!rs.next()) {
            throw new java.util.NoSuchElementException("No open draw configured for " + gameCode);
          }
          return new PosAvailableDrawView(
              DrawId.of(rs.getObject("draw_id", UUID.class)),
              DrawChannelId.of(rs.getObject("draw_channel_id", UUID.class)),
              null,
              null,
              null,
              null,
              null,
              List.of(gameCode.name()),
              "OPEN",
              rs.getTimestamp("scheduled_at").toInstant(),
              rs.getTimestamp("cutoff_at").toInstant(),
              LocalDate.of(2026, 7, 19),
              LocalTime.of(18, 0),
              "America/Port-au-Prince",
              LocalDate.of(2026, 7, 19),
              LocalTime.of(18, 0),
              "America/Port-au-Prince");
        },
        tenantId.value(),
        Timestamp.from(SALES_NOW),
        gameCode.name());
  }

  private Integer countTickets() {
    return jdbc.queryForObject(
        "select count(*) from sales_ticket where tenant_id = ?", Integer.class, tenantId.value());
  }
}
