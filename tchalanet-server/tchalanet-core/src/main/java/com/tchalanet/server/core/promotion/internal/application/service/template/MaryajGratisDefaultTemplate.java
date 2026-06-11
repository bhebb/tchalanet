package com.tchalanet.server.core.promotion.internal.application.service.template;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleConfigInput;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
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
        return new CreatePromotionCampaignCommand(
            tenantId,
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
                    Map.of(
                        "gameCode", "HT_MARYAJ_GRATUIT",
                        "payoutBaseAmount", DEFAULT_PAYOUT_BASE_AMOUNT,
                        "quantity", "1",
                        "choiceMode", "AUTO_GENERATE",
                        "generationStrategy", "RANDOM",
                        "regenerableBeforeConfirm", "true",
                        "maxRegenerationsBeforeConfirm", "3"
                    )
                ))
            ))
        );
    }
}
