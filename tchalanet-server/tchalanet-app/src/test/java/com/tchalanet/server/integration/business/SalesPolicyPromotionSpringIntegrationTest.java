package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitLineContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.sales.api.command.preparation.ConfirmPreparedSaleCommand;
import com.tchalanet.server.core.sales.api.command.preparation.PrepareSaleCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.features.pos.draws.PosAvailableDrawView;
import com.tchalanet.server.features.pos.draws.PosDrawsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Sales policy and promotion Spring integration")
class SalesPolicyPromotionSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private PosDrawsService posDrawsService;

    @Test
    @DisplayName("should reject blocked selection without persisting a ticket")
    void shouldRejectBlockedSelectionWithoutPersistingTicket() throws Exception {
        var draw = openedDrawContaining(GameCode.HT_BOLET);
        var before = countTickets();

        withContext(tenantAdminContext, () -> commandBus.execute(new UpsertLimitAssignmentCommand(
            tenantId,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            LimitScopeRef.drawChannel(draw.drawChannelId()),
            true,
            BreachOutcome.BLOCK,
            JSON.readTree("""
                {"betType":"MATCH_1_2D","selections":["05"]}
                """),
            FIXED_NOW.minusSeconds(86_400),
            FIXED_NOW.plusSeconds(86_400)
        )));
        assertThat(limitAssignmentsFor(draw)).isEqualTo(1);

        var directEvaluation = withContext(sellerContext, () -> queryBus.ask(new EvaluateLimitPolicyQuery(
            new LimitContext(
                tenantId,
                null,
                sellerContext.sellerTerminalIdRequired(),
                draw.drawId(),
                draw.drawChannelId(),
                FIXED_NOW,
                List.of(new LimitLineContext(BetType.MATCH_1_2D, "05", 1000, 60000))))));
        assertThat(directEvaluation.outcome()).isEqualTo(BreachOutcome.BLOCK);

        var result = withContext(sellerContext, () -> commandBus.execute(new SellTicketCommand(
            draw.drawId(),
            draw.drawChannelId(),
            HTG,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_1_2D,
                "05",
                null,
                new BigDecimal("10"))),
            SaleCommunicationOptions.none(),
            List.of()
        )));

        assertThat(result.outcome()).isEqualTo(SellTicketOutcome.REJECTED);
        assertThat(result.issues())
            .isNotEmpty()
            .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("sales.selection_exposure_limit_exceeded"));
        assertThat(countTickets()).isEqualTo(before);
    }

    @Test
    @DisplayName("should persist Maryaj gratis tiers and generate promotion lines during prepared sale")
    void shouldPersistMaryajGratisTiersAndGeneratePromotionLines() {
        var draw = openedDrawContaining(GameCode.HT_BOLET);
        var campaign = ensureMaryajGratisCampaign();
        var rule = campaign.rules().getFirst();

        var updated = withContext(tenantAdminContext, () ->
            commandBus.execute(new UpdatePromotionRuleEffectsCommand(
                tenantId,
                campaign.id(),
                rule.id(),
                List.of(new PromotionEffectConfigInput(PromotionEffectType.FREE_GAME_LINE, Map.of(
                    "gameCode", "HT_MARYAJ_GRATIS",
                    "payoutBaseAmount", "75",
                    "quantity", "1",
                    "quantityMode", PromotionQuantityMode.TIERED_PAID_AMOUNT.name(),
                    "maxQuantity", "2",
                    "quantityTiers", List.of(
                        Map.of("minPaidAmount", "100", "maxPaidAmount", "499", "quantity", "1"),
                        Map.of("minPaidAmount", "500", "quantity", "2")
                    ),
                    "choiceMode", PromotionChoiceMode.AUTO_GENERATE.name(),
                    "generationStrategy", "RANDOM",
                    "regenerableBeforeConfirm", "true",
                    "maxRegenerationsBeforeConfirm", "3"
                )))
            )));

        assertThat(updated.rules().getFirst().effects().getFirst().params())
            .containsEntry("payoutBaseAmount", new BigDecimal("75"))
            .containsEntry("maxQuantity", 2);
        assertThat(updated.rules().getFirst().effects().getFirst().params().get("quantityTiers"))
            .asList()
            .hasSize(2);

        var preparation = withContext(sellerContext, () -> commandBus.execute(new PrepareSaleCommand(
            draw.drawId(),
            draw.drawChannelId(),
            HTG,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_1_2D,
                "12",
                null,
                new BigDecimal("500"))),
            SaleCommunicationOptions.none()
        )));

        assertThat(preparation.promotionLines()).hasSize(2);
        assertThat(preparation.lines())
            .filteredOn(line -> "PROMOTION".equals(line.origin()))
            .hasSize(2)
            .allSatisfy(line -> {
                assertThat(line.gameCode()).isEqualTo("HT_MARYAJ_GRATIS");
                assertThat(line.stakeAmount()).isEqualByComparingTo("0");
            });

        var confirmed = withContext(sellerContext, () -> commandBus.execute(
            new ConfirmPreparedSaleCommand(preparation.preparationId(), "maryaj-gratis-it-1")));

        assertThat(confirmed.ticketId()).isNotNull();
        assertThat(countTicketLines(confirmed.ticketId())).isEqualTo(3);
        assertThat(countPromotionTicketLines(confirmed.ticketId())).isEqualTo(2);
    }

    private PromotionCampaignView ensureMaryajGratisCampaign() {
        return withContext(tenantAdminContext, () ->
            commandBus.execute(new InstantiateDefaultMaryajGratisCommand(tenantId)));
    }

    private PosAvailableDrawView openedDrawContaining(GameCode gameCode) {
        var saleDate = LocalDate.of(2026, 7, 9);
        withContext(tenantAdminContext, () -> commandBus.execute(new GenerateDrawsForRangeCommand(
            tenantId,
            saleDate,
            saleDate,
            false,
            false,
            null)));
        withContext(tenantAdminContext, () -> commandBus.execute(new OpenDueDrawsCommand(
            FIXED_NOW,
            100,
            48,
            1,
            false)));

        return withContext(sellerContext, () ->
            posDrawsService.listAvailable(sellerContext, 48, 50).stream()
                .filter(draw -> draw.gameCodes().contains(gameCode.name()))
                .findFirst()
                .orElseThrow());
    }

    private Integer countTickets() {
        return jdbc.queryForObject(
            "select count(*) from sales_ticket where tenant_id = ?",
            Integer.class,
            tenantId.value());
    }

    private Integer limitAssignmentsFor(PosAvailableDrawView draw) {
        return jdbc.queryForObject(
            """
            select count(*)
            from limit_assignment
            where tenant_id = ?
              and rule_key = 'BLOCK_SELECTION_PER_DRAW'
              and scope_type = 'DRAW_CHANNEL'
              and scope_id = ?
              and deleted_at is null
            """,
            Integer.class,
            tenantId.value(),
            draw.drawChannelId().value());
    }

    private Integer countTicketLines(java.util.UUID ticketId) {
        return jdbc.queryForObject(
            "select count(*) from sales_ticket_line where ticket_id = ?",
            Integer.class,
            ticketId);
    }

    private Integer countPromotionTicketLines(java.util.UUID ticketId) {
        return jdbc.queryForObject(
            """
            select count(*)
            from sales_ticket_line
            where ticket_id = ?
              and origin = 'PROMOTION'
              and game_code = 'HT_MARYAJ_GRATIS'
            """,
            Integer.class,
            ticketId);
    }
}
