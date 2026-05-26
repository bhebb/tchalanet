package com.tchalanet.server.core.promotion.internal.application.service.lifecycle;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleView;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PromotionCampaignActivationPolicy {

    private static final Set<PromotionEffectType> V1_SUPPORTED_EFFECTS =
        Set.of(PromotionEffectType.WAIVE_CHARGE, PromotionEffectType.BOOST_ODDS, PromotionEffectType.FREE_GAME_LINE);

    public void validate(PromotionCampaignView campaign) {
        validateDates(campaign);
        validateActiveRules(campaign);
        // TODO: validate required game codes exist and are enabled for tenant (game catalog validator not available in V1)
    }

    private void validateDates(PromotionCampaignView campaign) {
        if (campaign.startsAt() == null || campaign.endsAt() == null) {
            throw ProblemRest.badRequest("promotion.campaign.dates_required_for_activation");
        }
        if (!campaign.startsAt().isBefore(campaign.endsAt())) {
            throw ProblemRest.badRequest("promotion.campaign.start_must_be_before_end");
        }
    }

    private void validateActiveRules(PromotionCampaignView campaign) {
        var rules = campaign.rules() == null
            ? List.<PromotionRuleView>of()
            : campaign.rules();

        if (rules.isEmpty()) {
            throw ProblemRest.badRequest("promotion.campaign.no_rules");
        }

        for (var rule : rules) {
            if (rule.effects() == null || rule.effects().isEmpty()) {
                throw ProblemRest.badRequest("promotion.campaign.rule_missing_effects");
            }
            for (var effect : rule.effects()) {
                if (effect.type() == null || !V1_SUPPORTED_EFFECTS.contains(effect.type())) {
                    throw ProblemRest.badRequest("promotion.campaign.rule_effect_type_unsupported");
                }
            }
        }
    }
}
