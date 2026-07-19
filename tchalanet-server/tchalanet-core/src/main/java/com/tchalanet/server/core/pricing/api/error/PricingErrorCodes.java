package com.tchalanet.server.core.pricing.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by tenant and seller-terminal pricing. */
@UtilityClass
public class PricingErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES = Set.of(ErrorAudience.WEB_ADMIN);

  public static final ErrorDescriptor TENANT_REQUIRED = validation("pricing.tenant_required");
  public static final ErrorDescriptor GAME_CODE_REQUIRED = validation("pricing.game_code_required");
  public static final ErrorDescriptor PRICING_VARIANT_REQUIRED =
      validation("pricing.pricing_variant_required");
  public static final ErrorDescriptor BET_TYPE_REQUIRED = validation("pricing.bet_type_required");
  public static final ErrorDescriptor ODDS_INVALID = validation("pricing.odds_invalid");
  public static final ErrorDescriptor FIXED_AMOUNT_INVALID =
      validation("pricing.fixed_amount_invalid");
  public static final ErrorDescriptor PAYOUT_RULE_TYPE_INVALID =
      validation("pricing.payout_rule_type_invalid");
  public static final ErrorDescriptor TENANT_DEFAULT_NOT_CONFIGURED =
      conflict("pricing.tenant_default_not_configured");
  public static final ErrorDescriptor OVERRIDE_NOT_FOUND = notFound("pricing.override_not_found");
  public static final ErrorDescriptor OVERRIDE_PAYOUT_RULE_TYPE_MISMATCH =
      conflict("pricing.override_payout_rule_type_mismatch");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        TENANT_REQUIRED,
        GAME_CODE_REQUIRED,
        PRICING_VARIANT_REQUIRED,
        BET_TYPE_REQUIRED,
        ODDS_INVALID,
        FIXED_AMOUNT_INVALID,
        PAYOUT_RULE_TYPE_INVALID,
        TENANT_DEFAULT_NOT_CONFIGURED,
        OVERRIDE_NOT_FOUND,
        OVERRIDE_PAYOUT_RULE_TYPE_MISMATCH);
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
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
