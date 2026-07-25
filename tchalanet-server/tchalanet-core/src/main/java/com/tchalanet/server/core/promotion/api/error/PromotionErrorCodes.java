package com.tchalanet.server.core.promotion.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing errors for promotion campaigns and Maryaj gratis configuration. */
@UtilityClass
public class PromotionErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE);

  public static final ErrorDescriptor CAMPAIGN_NOT_FOUND = notFound("promotion.campaign.not_found");
  public static final ErrorDescriptor RULE_NOT_FOUND = notFound("promotion.rule.not_found");
  public static final ErrorDescriptor RULE_KEY_ALREADY_EXISTS =
      conflict("promotion.rule.rule_key_already_exists");

  private static final Set<String> VALIDATION_CODES =
      Set.of(
          "promotion.campaign.dates_required_for_activation",
          "promotion.campaign.start_must_be_before_end",
          "promotion.campaign.no_rules",
          "promotion.campaign.rule_missing_effects",
          "promotion.campaign.rule_effect_type_unsupported",
          "promotion.campaign.rule_generation_strategy_unsupported",
          "promotion.campaign.rules_edit_requires_draft",
          "promotion.campaign.rules_required",
          "promotion.maryaj_gratis.choice_mode_invalid",
          "promotion.maryaj_gratis.generation_strategy_unsupported",
          "promotion.maryaj_gratis.generation_strategy_requires_auto_generate",
          "promotion.maryaj_gratis.regeneration_requires_auto_generate",
          "promotion.maryaj_gratis.step_paid_amount_required",
          "promotion.maryaj_gratis.quantity_tiers_required",
          "promotion.rule.rule_key_required",
          "promotion.rule.eligibility_required",
          "promotion.rule.eligibility_type_required",
          "promotion.rule.effects_required",
          "promotion.rule.effect_type_required",
          "promotion.rule.boost_odds_scale",
          "promotion.rule.quantity_mode_invalid",
          "promotion.rule.quantity_tiers_required",
          "promotion.rule.quantity_tiers_invalid",
          "promotion.rule.quantity_tier_range_invalid",
          "promotion.rule.choice_mode_invalid",
          "promotion.rule.generation_strategy_invalid",
          "promotion.rule.generation_strategy_unsupported",
          "promotion.rule.generation_strategy_requires_auto_generate",
          "promotion.rule.priority_must_be_non_negative",
          "promotion.rule.value_must_be_positive",
          "promotion.rule.value_must_be_non_negative",
          "promotion.rule.required_field_missing");

  public static ErrorDescriptor validation(String code) {
    if (!VALIDATION_CODES.contains(code)) {
      throw new IllegalArgumentException("Unknown promotion validation code");
    }
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }

  public static Set<ErrorDescriptor> all() {
    return Stream.concat(
            Stream.of(CAMPAIGN_NOT_FOUND, RULE_NOT_FOUND, RULE_KEY_ALREADY_EXISTS),
            VALIDATION_CODES.stream().map(PromotionErrorCodes::validation))
        .collect(Collectors.toUnmodifiableSet());
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }
}
