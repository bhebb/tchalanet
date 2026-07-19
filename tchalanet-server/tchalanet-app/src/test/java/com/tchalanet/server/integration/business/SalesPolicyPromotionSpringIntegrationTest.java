package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
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
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantPricingRuleCommand;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.sales.api.command.preparation.ConfirmPreparedSaleCommand;
import com.tchalanet.server.core.sales.api.command.preparation.PrepareSaleCommand;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptPrintQuery;
import com.tchalanet.server.core.sales.internal.application.command.model.ReprintTicketCommand;
import com.tchalanet.server.features.pos.draws.PosAvailableDrawView;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import com.tchalanet.server.platform.document.api.model.PrintOptionsRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Sales policy and promotion Spring integration")
class SalesPolicyPromotionSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final ZoneId TENANT_ZONE = ZoneId.of("America/Port-au-Prince");
  private static final Instant SALES_NOW = Instant.now();

  @Test
  @DisplayName("should reject blocked selection without persisting a ticket")
  void shouldRejectBlockedSelectionWithoutPersistingTicket() throws Exception {
    var draw = openedDrawContaining(GameCode.HT_BOLET);
    var before = countTickets();

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertLimitAssignmentCommand(
                    tenantId,
                    RuleKey.BLOCK_SELECTION_PER_DRAW,
                    LimitScopeRef.drawChannel(draw.drawChannelId()),
                    true,
                    BreachOutcome.BLOCK,
                    JSON.readTree(
                        """
                {"betType":"MATCH_1_2D","selections":["05"]}
                """),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400))));
    assertThat(limitAssignmentsFor(draw)).isEqualTo(1);

    var directEvaluation =
        withContext(
            sellerContext,
            () ->
                queryBus.ask(
                    new EvaluateLimitPolicyQuery(
                        new LimitContext(
                            tenantId,
                            null,
                            sellerContext.sellerTerminalIdRequired(),
                            draw.drawId(),
                            draw.drawChannelId(),
                            SALES_NOW,
                            List.of(new LimitLineContext(BetType.MATCH_1_2D, "05", 1000))))));
    assertThat(directEvaluation.outcome()).isEqualTo(BreachOutcome.BLOCK);

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
                                "05",
                                null,
                                new BigDecimal("10"))),
                        SaleCommunicationOptions.none(),
                        List.of())));

    assertThat(result.outcome()).isEqualTo(SellTicketOutcome.REJECTED);
    assertThat(result.issues())
        .isNotEmpty()
        .anySatisfy(
            issue -> assertThat(issue.code()).isEqualTo("sales.selection_exposure_limit_exceeded"));
    assertThat(countTickets()).isEqualTo(before);
  }

  @Test
  @DisplayName(
      "should persist Maryaj gratis tiers and generate promotion lines during prepared sale")
  void shouldPersistMaryajGratisTiersAndGeneratePromotionLines() {
    var draw = openedDrawContaining(GameCode.HT_BOLET);
    var campaign = ensureMaryajGratisCampaign();
    var rule = campaign.rules().getFirst();

    var updated =
        withContext(
            tenantAdminContext,
            () ->
                commandBus.execute(
                    new UpdatePromotionRuleEffectsCommand(
                        tenantId,
                        campaign.id(),
                        rule.id(),
                        List.of(
                            new PromotionEffectConfigInput(
                                PromotionEffectType.FREE_GAME_LINE,
                                Map.of(
                                    "gameCode", "HT_MARYAJ_GRATIS",
                                    "payoutBaseAmount", "75",
                                    "quantity", "1",
                                    "quantityMode", PromotionQuantityMode.TIERED_PAID_AMOUNT.name(),
                                    "maxQuantity", "2",
                                    "quantityTiers",
                                        List.of(
                                            Map.of(
                                                "minPaidAmount",
                                                "100",
                                                "maxPaidAmount",
                                                "499",
                                                "quantity",
                                                "1"),
                                            Map.of("minPaidAmount", "500", "quantity", "2")),
                                    "choiceMode", PromotionChoiceMode.AUTO_GENERATE.name(),
                                    "generationStrategy", "RANDOM",
                                    "regenerableBeforeConfirm", "true",
                                    "maxRegenerationsBeforeConfirm", "3"))))));

    assertThat(updated.rules().getFirst().effects().getFirst().params())
        .containsEntry("payoutBaseAmount", new BigDecimal("75"))
        .containsEntry("maxQuantity", 2);
    assertThat(updated.rules().getFirst().effects().getFirst().params().get("quantityTiers"))
        .asList()
        .hasSize(2);

    var preparation =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new PrepareSaleCommand(
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
                                new BigDecimal("500"))),
                        SaleCommunicationOptions.none())));

    assertThat(preparation.promotionLines()).hasSize(2);
    assertThat(preparation.lines())
        .filteredOn(line -> "PROMOTION".equals(line.origin()))
        .hasSize(2)
        .allSatisfy(
            line -> {
              assertThat(line.gameCode()).isEqualTo("HT_MARYAJ_GRATIS");
              assertThat(line.stakeAmount()).isEqualByComparingTo("0");
            });

    var confirmed =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new ConfirmPreparedSaleCommand(
                        preparation.preparationId(), "maryaj-gratis-it-1")));

    assertThat(confirmed.ticketId()).isNotNull();
    assertThat(countTicketLines(confirmed.ticketId())).isEqualTo(3);
    assertThat(countPromotionTicketLines(confirmed.ticketId())).isEqualTo(2);
  }

  @Test
  @DisplayName("should snapshot Maryaj gratis terminal override and ignore later pricing changes")
  void shouldSnapshotMaryajGratisTerminalOverrideAndIgnoreLaterPricingChanges() {
    var draw = openedDrawContaining(GameCode.HT_BOLET);
    var campaign = ensureMaryajGratisCampaign();
    configureSingleMaryajGratisLine(campaign);
    upsertMaryajGratisTenantRule("1000");
    upsertMaryajGratisTerminalOverride("2000", "initial terminal override");

    var firstTicket = sellBoletWithMaryajGratis(draw, "maryaj-gratis-snapshot-it-1");
    assertMaryajGratisSnapshot(firstTicket, "2000", "SELLER_TERMINAL_OVERRIDE");

    upsertMaryajGratisTenantRule("3000");
    upsertMaryajGratisTerminalOverride("4000", "updated terminal override");

    assertMaryajGratisSnapshot(firstTicket, "2000", "SELLER_TERMINAL_OVERRIDE");

    var secondTicket = sellBoletWithMaryajGratis(draw, "maryaj-gratis-snapshot-it-2");
    assertMaryajGratisSnapshot(secondTicket, "4000", "SELLER_TERMINAL_OVERRIDE");
  }

  @Test
  @DisplayName("should format original and reprint receipts with Maryaj gratis promotion")
  void shouldFormatOriginalAndReprintReceiptsWithMaryajGratisPromotion() {
    setTenantReceiptMessages("IT HEADER MARYAJ GRATIS", "IT FOOTER MARYAJ GRATIS");
    var draw = openedDrawContaining(GameCode.HT_BOLET);
    var campaign = ensureMaryajGratisCampaign();
    configureSingleMaryajGratisLine(campaign);
    upsertMaryajGratisTenantRule("1000");
    upsertMaryajGratisTerminalOverride("2000", "print receipt terminal override");

    var ticketId = sellBoletWithMaryajGratis(draw, "maryaj-gratis-print-it-1");

    var original = formatReceipt(ticketId);
    assertReceiptContainsTenantBrandingAndMaryajGratis(original);
    assertCopyMarker(original, "original", "originale");
    assertMaryajGratisSnapshot(ticketId, "2000", "SELLER_TERMINAL_OVERRIDE");

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new RecordTicketPrintCommand(
                    TicketId.of(ticketId),
                    new PrintOptionsRequest(DocumentFormat.ESC_POS, PaperSize.RECEIPT_80MM),
                    "integration first print",
                    UserId.of(ADMIN_USER_ID),
                    null)));
    upsertMaryajGratisTenantRule("3000");
    upsertMaryajGratisTerminalOverride("4000", "pricing changed after first print");

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new ReprintTicketCommand(
                    tenantId, TicketId.of(ticketId), UserId.of(ADMIN_USER_ID))));
    var duplicate = formatReceipt(ticketId);

    assertReceiptContainsTenantBrandingAndMaryajGratis(duplicate);
    assertCopyMarker(duplicate, "copy", "copie");
    assertMaryajGratisSnapshot(ticketId, "2000", "SELLER_TERMINAL_OVERRIDE");
  }

  private PromotionCampaignView ensureMaryajGratisCampaign() {
    return withContext(
        tenantAdminContext,
        () -> commandBus.execute(new InstantiateDefaultMaryajGratisCommand(tenantId)));
  }

  private void configureSingleMaryajGratisLine(PromotionCampaignView campaign) {
    var rule = campaign.rules().getFirst();
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpdatePromotionRuleEffectsCommand(
                    tenantId,
                    campaign.id(),
                    rule.id(),
                    List.of(
                        new PromotionEffectConfigInput(
                            PromotionEffectType.FREE_GAME_LINE,
                            Map.of(
                                "gameCode", "HT_MARYAJ_GRATIS",
                                "payoutBaseAmount", "1",
                                "quantity", "1",
                                "quantityMode", PromotionQuantityMode.TIERED_PAID_AMOUNT.name(),
                                "maxQuantity", "1",
                                "quantityTiers",
                                    List.of(Map.of("minPaidAmount", "100", "quantity", "1")),
                                "choiceMode", PromotionChoiceMode.AUTO_GENERATE.name(),
                                "generationStrategy", "RANDOM",
                                "regenerableBeforeConfirm", "true",
                                "maxRegenerationsBeforeConfirm", "3"))))));
  }

  private void upsertMaryajGratisTenantRule(String fixedAmount) {
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertTenantPricingRuleCommand(
                    tenantId,
                    GameCode.HT_MARYAJ_GRATIS.name(),
                    PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
                    BetType.MARRIAGE_2D2D.name(),
                    (short) 2,
                    null,
                    PayoutRuleType.FIXED_AMOUNT,
                    new BigDecimal(fixedAmount),
                    UserId.of(ADMIN_USER_ID))));
  }

  private void upsertMaryajGratisTerminalOverride(String fixedAmount, String reason) {
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new UpsertSellerTerminalPricingRuleOverrideCommand(
                    tenantId,
                    SellerTerminalId.of(SELLER_TERMINAL_ID),
                    GameCode.HT_MARYAJ_GRATIS.name(),
                    PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
                    BetType.MARRIAGE_2D2D.name(),
                    (short) 2,
                    null,
                    PayoutRuleType.FIXED_AMOUNT,
                    new BigDecimal(fixedAmount),
                    FIXED_NOW.minusSeconds(86_400),
                    SALES_NOW.plusSeconds(86_400),
                    reason,
                    UserId.of(ADMIN_USER_ID))));
  }

  private void setTenantReceiptMessages(String header, String footer) {
    jdbc.update(
        """
            update tenant
            set config = coalesce(config, '{}'::jsonb)
                || jsonb_build_object(
                    'document',
                    jsonb_build_object(
                        'receipt',
                        jsonb_build_object(
                            'headerMessage', ?,
                            'footerMessage', ?
                        )
                    )
                ),
                updated_at = ?
            where id = ?
            """,
        header,
        footer,
        Timestamp.from(SALES_NOW),
        tenantId.value());
  }

  private UUID sellBoletWithMaryajGratis(PosAvailableDrawView draw, String idempotencyKey) {
    var preparation =
        withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new PrepareSaleCommand(
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
                        SaleCommunicationOptions.none())));

    assertThat(preparation.promotionLines()).hasSize(1);

    return withContext(
            sellerContext,
            () ->
                commandBus.execute(
                    new ConfirmPreparedSaleCommand(preparation.preparationId(), idempotencyKey)))
        .ticketId();
  }

  private void assertMaryajGratisSnapshot(UUID ticketId, String fixedAmount, String source) {
    assertThat(maryajGratisSnapshotRuleType(ticketId)).isEqualTo("FIXED_AMOUNT");
    assertThat(maryajGratisSnapshotFixedAmount(ticketId)).isEqualByComparingTo(fixedAmount);
    assertThat(maryajGratisSnapshotSource(ticketId)).isEqualTo(source);
  }

  private TicketReceiptPrintContent formatReceipt(UUID ticketId) {
    return withContext(
        tenantAdminContext,
        () ->
            queryBus.ask(
                new FormatTicketReceiptPrintQuery(
                    TicketId.of(ticketId),
                    DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_80MM),
                    Locale.FRENCH)));
  }

  private void assertReceiptContainsTenantBrandingAndMaryajGratis(
      TicketReceiptPrintContent content) {
    assertThat(content.headerLines())
        .anySatisfy(line -> assertThat(line.text()).contains("IT HEADER MARYAJ GRATIS"));
    assertThat(content.footerLines())
        .anySatisfy(line -> assertThat(line.text()).contains("IT FOOTER MARYAJ GRATIS"));
    assertThat(content.qrPayload()).contains(content.metadata().get("publicCode"));

    var body = content.bodyLines();
    assertThat(body).anySatisfy(line -> assertThat(line).contains("GRATIS"));
    // Since #332, the free maryaj prints in its own section without the
    // "* Maryaj offert" note (see TicketReceiptGameLinesFormatter).
    assertThat(body).noneSatisfy(line -> assertThat(line).contains("* Maryaj offert"));
  }

  private void assertCopyMarker(TicketReceiptPrintContent content, String english, String french) {
    assertThat(content.headerLines())
        .first()
        .satisfies(
            line ->
                assertThat(line.text().toLowerCase(Locale.ROOT))
                    .satisfiesAnyOf(
                        text -> assertThat(text).contains(english),
                        text -> assertThat(text).contains(french)));
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

  private Integer countTicketLines(UUID ticketId) {
    return jdbc.queryForObject(
        "select count(*) from sales_ticket_line where ticket_id = ?", Integer.class, ticketId);
  }

  private Integer countPromotionTicketLines(UUID ticketId) {
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

  private String maryajGratisSnapshotRuleType(UUID ticketId) {
    return jdbc.queryForObject(
        """
            select settlement_terms_snapshot #>> '{terms,0,payoutRule,type}'
            from sales_ticket_line
            where ticket_id = ?
              and origin = 'PROMOTION'
              and game_code = 'HT_MARYAJ_GRATIS'
            order by line_number
            limit 1
            """,
        String.class,
        ticketId);
  }

  private BigDecimal maryajGratisSnapshotFixedAmount(UUID ticketId) {
    return jdbc.queryForObject(
        """
            select (settlement_terms_snapshot #>> '{terms,0,payoutRule,fixedAmount}')::numeric
            from sales_ticket_line
            where ticket_id = ?
              and origin = 'PROMOTION'
              and game_code = 'HT_MARYAJ_GRATIS'
            order by line_number
            limit 1
            """,
        BigDecimal.class,
        ticketId);
  }

  private String maryajGratisSnapshotSource(UUID ticketId) {
    return jdbc.queryForObject(
        """
            select settlement_terms_snapshot #>> '{terms,0,source}'
            from sales_ticket_line
            where ticket_id = ?
              and origin = 'PROMOTION'
              and game_code = 'HT_MARYAJ_GRATIS'
            order by line_number
            limit 1
            """,
        String.class,
        ticketId);
  }
}
