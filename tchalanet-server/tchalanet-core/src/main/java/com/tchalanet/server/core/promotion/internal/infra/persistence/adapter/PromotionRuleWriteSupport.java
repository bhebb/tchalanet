package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.DeletePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleConfigInput;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEffectJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEligibilityLineJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEffectRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEligibilityLineRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PromotionRuleWriteSupport {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRuleRepository ruleRepository;
    private final PromotionRuleEffectRepository effectRepository;
    private final PromotionRuleEligibilityLineRepository eligibilityLineRepository;
    private final PromotionCampaignViewAssembler viewAssembler;

    PromotionCampaignView addRule(AddPromotionRuleCommand cmd) {
        var campaign = campaignRepository.getRequired(cmd.campaignId().value());
        requireDraft(campaign);
        addRule(campaign, new PromotionRuleConfigInput(
            cmd.ruleKey(),
            cmd.priority(),
            cmd.eligibilityItems(),
            cmd.effectItems()
        ));

        return viewAssembler.toCampaignView(campaign.getId());
    }

    void addRules(PromotionCampaignJpaEntity campaign, List<PromotionRuleConfigInput> rules) {
        if (rules == null || rules.isEmpty()) {
            throw ProblemRest.badRequest("promotion.campaign.rules_required");
        }
        requireDraft(campaign);
        rules.forEach(rule -> addRule(campaign, rule));
    }

    private void addRule(PromotionCampaignJpaEntity campaign, PromotionRuleConfigInput input) {
        ensureRuleKeyAvailable(campaign.getId(), input.ruleKey());

        var rule = new PromotionRuleJpaEntity();
        rule.setCampaignId(campaign.getId());
        rule.setRuleKey(requiredText(input.ruleKey(), "promotion.rule.rule_key_required"));
        rule.setPriority(nonNegativePriority(input.priority()));

        var saved = ruleRepository.save(rule);
        applyEligibility(saved, input.eligibilityItems());
        ruleRepository.save(saved);
        replaceEffects(saved.getId(), input.effectItems());
    }

    PromotionCampaignView updateRule(UpdatePromotionRuleCommand cmd) {
        var campaign = campaignRepository.getRequired(cmd.campaignId().value());
        requireDraft(campaign);
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());

        if (cmd.ruleKey() != null && !cmd.ruleKey().isBlank() && !cmd.ruleKey().equals(rule.getRuleKey())) {
            ensureRuleKeyAvailable(cmd.campaignId().value(), cmd.ruleKey());
            rule.setRuleKey(cmd.ruleKey());
        }

        if (cmd.priority() != null) {
            rule.setPriority(nonNegativePriority(cmd.priority()));
        }

        ruleRepository.save(rule);
        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    PromotionCampaignView updateRuleEligibility(UpdatePromotionRuleEligibilityCommand cmd) {
        var campaign = campaignRepository.getRequired(cmd.campaignId().value());
        requireDraft(campaign);
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());
        applyEligibility(rule, cmd.items());
        ruleRepository.save(rule);
        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    PromotionCampaignView updateRuleEffects(UpdatePromotionRuleEffectsCommand cmd) {
        var campaign = campaignRepository.getRequired(cmd.campaignId().value());
        requireDraft(campaign);
        getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());
        replaceEffects(cmd.ruleId().value(), cmd.items());
        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    PromotionCampaignView deleteRule(DeletePromotionRuleCommand cmd) {
        var campaign = campaignRepository.getRequired(cmd.campaignId().value());
        requireDraft(campaign);
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());
        effectRepository.deleteByRuleId(rule.getId());
        eligibilityLineRepository.deleteByRuleId(rule.getId());
        ruleRepository.delete(rule);
        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    private PromotionRuleJpaEntity getRuleRequired(UUID campaignId, UUID ruleId) {
        return ruleRepository.findByIdAndCampaignId(ruleId, campaignId)
            .orElseThrow(() -> ProblemRest.notFound("promotion.rule.not_found"));
    }

    private void requireDraft(PromotionCampaignJpaEntity campaign) {
        if (campaign.getStatus() != PromotionCampaignStatus.DRAFT) {
            throw ProblemRest.badRequest("promotion.campaign.rules_edit_requires_draft");
        }
    }

    private void ensureRuleKeyAvailable(UUID campaignId, String ruleKey) {
        if (ruleKey == null || ruleKey.isBlank()) {
            throw ProblemRest.badRequest("promotion.rule.rule_key_required");
        }
        if (ruleRepository.existsByCampaignIdAndRuleKey(campaignId, ruleKey)) {
            throw ProblemRest.conflict("promotion.rule.rule_key_already_exists");
        }
    }

    private void applyEligibility(PromotionRuleJpaEntity rule, List<PromotionEligibilityConfigInput> items) {
        rule.setMinPaidTotal(null);
        rule.setBeforeLocalTime(null);
        eligibilityLineRepository.deleteByRuleId(rule.getId());

        if (items == null || items.isEmpty()) {
            throw ProblemRest.badRequest("promotion.rule.eligibility_required");
        }

        for (var item : items) {
            if (item.type() == null) {
                throw ProblemRest.badRequest("promotion.rule.eligibility_type_required");
            }
            switch (item.type()) {
                case MIN_PAID_TOTAL -> rule.setMinPaidTotal(positiveDecimal(item.params(), "amount", "minPaidTotal"));
                case BEFORE_LOCAL_TIME -> rule.setBeforeLocalTime(localTime(item.params(), "time", "beforeLocalTime"));
                case PAID_LINE_COUNT -> eligibilityLineRepository.save(eligibilityLine(rule.getId(), item.params()));
            }
        }
    }

    private PromotionRuleEligibilityLineJpaEntity eligibilityLine(UUID ruleId, Map<String, Object> params) {
        var line = new PromotionRuleEligibilityLineJpaEntity();
        line.setRuleId(ruleId);
        line.setGameCode(requiredString(params, "gameCode"));
        line.setMinCount(positiveInt(params, "minCount", "count"));
        return line;
    }

    private void replaceEffects(UUID ruleId, List<PromotionEffectConfigInput> items) {
        effectRepository.deleteByRuleId(ruleId);
        if (items == null || items.isEmpty()) {
            throw ProblemRest.badRequest("promotion.rule.effects_required");
        }
        items.stream()
            .map(item -> effect(ruleId, item))
            .forEach(effectRepository::save);
    }

    private PromotionRuleEffectJpaEntity effect(UUID ruleId, PromotionEffectConfigInput item) {
        if (item.type() == null) {
            throw ProblemRest.badRequest("promotion.rule.effect_type_required");
        }
        var effect = new PromotionRuleEffectJpaEntity();
        effect.setRuleId(ruleId);
        effect.setEffectType(item.type());
        var params = item.params() == null ? Map.<String, Object>of() : item.params();
        switch (item.type()) {
            case FREE_GAME_LINE -> {
                effect.setGameCode(requiredString(params, "gameCode"));
                effect.setPayoutBaseAmount(positiveDecimal(params, "payoutBaseAmount", "amount"));
                effect.setQuantity(params.containsKey("quantity") ? positiveInt(params, "quantity") : 1);
                applySelectionGeneration(effect, params);
            }
            case BOOST_ODDS -> {
                effect.setGameCode(requiredString(params, "gameCode"));
                effect.setOddsOverride(positiveDecimal(params, "oddsOverride"));
            }
            case WAIVE_CHARGE -> effect.setChargeType(requiredString(params, "chargeType", "chargeCode"));
        }
        return effect;
    }

    private void applySelectionGeneration(PromotionRuleEffectJpaEntity effect, Map<String, Object> params) {
        PromotionChoiceMode choiceMode = enumParam(
            params, "choiceMode", PromotionChoiceMode.class, "promotion.rule.choice_mode_invalid");
        SelectionGenerationStrategy strategy = enumParam(
            params, "generationStrategy", SelectionGenerationStrategy.class,
            "promotion.rule.generation_strategy_invalid");

        if (strategy == SelectionGenerationStrategy.LOW_EXPOSURE_RANDOM) {
            throw ProblemRest.badRequest("promotion.rule.generation_strategy_unsupported");
        }
        if (choiceMode == PromotionChoiceMode.AUTO_GENERATE && strategy == null) {
            strategy = SelectionGenerationStrategy.RANDOM;
        }
        if (strategy != null && choiceMode != PromotionChoiceMode.AUTO_GENERATE) {
            throw ProblemRest.badRequest("promotion.rule.generation_strategy_requires_auto_generate");
        }

        effect.setChoiceMode(choiceMode);
        effect.setGenerationStrategy(strategy);
        effect.setRegenerableBeforeConfirm(
            Boolean.parseBoolean(String.valueOf(params.getOrDefault("regenerableBeforeConfirm", "false"))));
        effect.setMaxRegenerationsBeforeConfirm(
            params.containsKey("maxRegenerationsBeforeConfirm")
                ? positiveInt(params, "maxRegenerationsBeforeConfirm")
                : 3);
    }

    private <E extends Enum<E>> E enumParam(
        Map<String, Object> params, String key, Class<E> type, String errorCode) {
        var raw = params.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            throw ProblemRest.badRequest(errorCode);
        }
    }

    private int nonNegativePriority(Integer priority) {
        if (priority == null || priority < 0) {
            throw ProblemRest.badRequest("promotion.rule.priority_must_be_non_negative");
        }
        return priority;
    }

    private BigDecimal positiveDecimal(Map<String, Object> params, String... keys) {
        var value = new BigDecimal(requiredString(params, keys));
        if (value.signum() <= 0) {
            throw ProblemRest.badRequest("promotion.rule.value_must_be_positive");
        }
        return value;
    }

    private int positiveInt(Map<String, Object> params, String... keys) {
        var value = Integer.parseInt(requiredString(params, keys));
        if (value <= 0) {
            throw ProblemRest.badRequest("promotion.rule.value_must_be_positive");
        }
        return value;
    }

    private LocalTime localTime(Map<String, Object> params, String... keys) {
        return LocalTime.parse(requiredString(params, keys));
    }

    private String requiredText(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw ProblemRest.badRequest(errorCode);
        }
        return value;
    }

    private String requiredString(Map<String, Object> params, String... keys) {
        for (var key : keys) {
            var value = params == null ? null : params.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        throw ProblemRest.badRequest("promotion.rule.required_field_missing");
    }
}
