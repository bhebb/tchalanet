package com.tchalanet.server.core.promotion.internal.application.service.template;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityType;
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

    /** minPaidTotal > 0 : any paid sale is eligible (at least one paid line). */
    static final String MIN_PAID_TOTAL = "1";

    static final Period VALIDITY = Period.ofYears(10);

    private MaryajGratisDefaultTemplate() {
    }

    public static CreatePromotionCampaignCommand createCommand(TenantId tenantId, Instant now) {
        return createCommand(new InstantiateDefaultMaryajGratisCommand(
            tenantId, null, null, null, null, null, null), now);
    }

    public static CreatePromotionCampaignCommand createCommand(
        InstantiateDefaultMaryajGratisCommand cmd,
        Instant now
    ) {
        var payoutBaseAmount = positiveAmountOrDefault(cmd.payoutBaseAmount(), DEFAULT_PAYOUT_BASE_AMOUNT);
        var quantity = positiveIntOrDefault(cmd.quantity(), 1);
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
        effectParams.put("quantity", String.valueOf(quantity));
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
}
