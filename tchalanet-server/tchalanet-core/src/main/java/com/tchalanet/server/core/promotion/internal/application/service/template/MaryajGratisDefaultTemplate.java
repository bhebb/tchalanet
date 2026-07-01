package com.tchalanet.server.core.promotion.internal.application.service.template;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityTier;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleConfigInput;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Versioned platform template for the default Maryaj gratuit campaign.
 * Lives in code (git-versioned seed); a dedicated template table is a later
 * evolution if needed. Each tenant gets its own instance and can then pause,
 * deactivate, or edit amount/eligibility independently.
 */
public final class MaryajGratisDefaultTemplate {

    public static final String CODE = "DEFAULT_MARYAJ_GRATIS";

    /** Default payout base amount (tenant currency, HTG for V1 tenants) — à confirmer métier. */
    static final String DEFAULT_PAYOUT_BASE_AMOUNT = "50";
    static final List<PromotionQuantityTier> DEFAULT_QUANTITY_TIERS = List.of(
        new PromotionQuantityTier(new BigDecimal("100"), new BigDecimal("199"), 1),
        new PromotionQuantityTier(new BigDecimal("200"), new BigDecimal("499"), 2),
        new PromotionQuantityTier(new BigDecimal("500"), null, 3)
    );

    /** minPaidTotal > 0 : any paid sale is eligible (at least one paid line). */
    static final String MIN_PAID_TOTAL = "1";
    static final int DEFAULT_QUANTITY_PER_STEP = 1;

    static final Period VALIDITY = Period.ofYears(10);

    private MaryajGratisDefaultTemplate() {
    }

    public static CreatePromotionCampaignCommand createCommand(TenantId tenantId, Instant now) {
        return createCommand(new InstantiateDefaultMaryajGratisCommand(
            tenantId, null, null, null, null, null, null, null, null, null, null, null), now);
    }

    public static CreatePromotionCampaignCommand createCommand(
        InstantiateDefaultMaryajGratisCommand cmd,
        Instant now
    ) {
        var payoutBaseAmount = positiveAmountOrDefault(cmd.payoutBaseAmount(), DEFAULT_PAYOUT_BASE_AMOUNT);
        var quantityMode = cmd.quantityMode() == null ? PromotionQuantityMode.TIERED_PAID_AMOUNT : cmd.quantityMode();
        var quantity = positiveIntOrDefault(cmd.quantity(), 1);
        var quantityPerStep = positiveIntOrDefault(cmd.quantityPerStep(), DEFAULT_QUANTITY_PER_STEP);
        var maxQuantity = positiveIntOrDefault(cmd.maxQuantity(), quantity);
        var stepPaidAmount = positiveAmountOrDefault(cmd.stepPaidAmount(), null);
        if (quantityMode == PromotionQuantityMode.PER_PAID_AMOUNT && stepPaidAmount == null) {
            throw new IllegalArgumentException("stepPaidAmount is required for PER_PAID_AMOUNT Maryaj gratis");
        }
        var quantityTiers = quantityMode == PromotionQuantityMode.TIERED_PAID_AMOUNT
            ? quantityTiersOrDefault(cmd.quantityTiers())
            : List.<PromotionQuantityTier>of();
        if (quantityMode == PromotionQuantityMode.TIERED_PAID_AMOUNT) {
            maxQuantity = quantityTiers.stream().mapToInt(PromotionQuantityTier::quantity).max().orElse(quantity);
        }
        var choiceMode = cmd.choiceMode() == null ? PromotionChoiceMode.AUTO_GENERATE : cmd.choiceMode();
        var generationStrategy = cmd.generationStrategy();
        if (choiceMode == PromotionChoiceMode.AUTO_GENERATE && generationStrategy == null) {
            generationStrategy = SelectionGenerationStrategy.RANDOM;
        }
        var regenerable = cmd.regenerableBeforeConfirm() == null ? Boolean.TRUE : cmd.regenerableBeforeConfirm();
        var maxRegenerations = positiveIntOrDefault(cmd.maxRegenerationsBeforeConfirm(), 3);

        var effectParams = new LinkedHashMap<String, Object>();
        effectParams.put("gameCode", "HT_MARYAJ_GRATUIT");
        effectParams.put("payoutBaseAmount", payoutBaseAmount);
        effectParams.put("quantityMode", quantityMode.name());
        effectParams.put("quantity", String.valueOf(quantity));
        if (quantityMode == PromotionQuantityMode.PER_PAID_AMOUNT) {
            effectParams.put("stepPaidAmount", stepPaidAmount);
            effectParams.put("quantityPerStep", String.valueOf(quantityPerStep));
            effectParams.put("maxQuantity", String.valueOf(maxQuantity));
        }
        if (quantityMode == PromotionQuantityMode.TIERED_PAID_AMOUNT) {
            effectParams.put("quantityTiers", quantityTiers.stream()
                .map(MaryajGratisDefaultTemplate::quantityTierParams)
                .toList());
            effectParams.put("maxQuantity", String.valueOf(maxQuantity));
        }
        effectParams.put("choiceMode", choiceMode.name());
        if (generationStrategy != null) {
            effectParams.put("generationStrategy", generationStrategy.name());
        }
        effectParams.put("regenerableBeforeConfirm", String.valueOf(regenerable));
        effectParams.put("maxRegenerationsBeforeConfirm", String.valueOf(maxRegenerations));

        return new CreatePromotionCampaignCommand(
            cmd.tenantId(),
            CODE,
            "Maryaj gratuit offert sur toute vente payante",
            now,
            now.atOffset(ZoneOffset.UTC).plus(VALIDITY).toInstant(),
            100,
            List.of(new PromotionRuleConfigInput(
                "maryaj-gratis-default",
                100,
                List.of(new PromotionEligibilityConfigInput(
                    PromotionEligibilityType.MIN_PAID_TOTAL,
                    Map.of("amount", MIN_PAID_TOTAL)
                )),
                List.of(new PromotionEffectConfigInput(
                    PromotionEffectType.FREE_GAME_LINE,
                    effectParams
                ))
            ))
        );
    }

    private static String positiveAmountOrDefault(BigDecimal value, String defaultValue) {
        if (value == null || value.signum() <= 0) {
            return defaultValue;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static int positiveIntOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private static List<PromotionQuantityTier> quantityTiersOrDefault(List<PromotionQuantityTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return DEFAULT_QUANTITY_TIERS;
        }
        return tiers;
    }

    private static Map<String, Object> quantityTierParams(PromotionQuantityTier tier) {
        var params = new LinkedHashMap<String, Object>();
        params.put("minPaidAmount", tier.minPaidAmount().stripTrailingZeros().toPlainString());
        if (tier.maxPaidAmount() != null) {
            params.put("maxPaidAmount", tier.maxPaidAmount().stripTrailingZeros().toPlainString());
        }
        params.put("quantity", String.valueOf(tier.quantity()));
        return params;
    }
}
