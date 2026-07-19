package com.tchalanet.server.platform.tenantgame.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures for tenant game configuration. */
@UtilityClass
public class TenantGameErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor CATALOG_GAME_NOT_FOUND =
      notFound("tenantgame.catalog_game_not_found");
  public static final ErrorDescriptor NOT_FOUND = notFound("tenantgame.not_found");
  public static final ErrorDescriptor DISABLED = conflict("tenantgame.disabled");
  public static final ErrorDescriptor CATALOG_GAME_INACTIVE =
      conflict("tenantgame.catalog_game_inactive");
  public static final ErrorDescriptor DISPLAY_NAME_TOO_LONG =
      validation("tenantgame.display_name_too_long");
  public static final ErrorDescriptor DISPLAY_ORDER_INVALID =
      validation("tenantgame.display_order_invalid");
  public static final ErrorDescriptor MIN_STAKE_INVALID =
      validation("tenantgame.min_stake_invalid");
  public static final ErrorDescriptor MAX_STAKE_INVALID =
      validation("tenantgame.max_stake_invalid");
  public static final ErrorDescriptor STAKE_RANGE_INVALID =
      validation("tenantgame.stake_range_invalid");
  public static final ErrorDescriptor AVAILABILITY_DAY_INVALID =
      validation("tenantgame.availability_day_invalid");
  public static final ErrorDescriptor TIME_INVALID = validation("tenantgame.time_invalid");
  public static final ErrorDescriptor BET_TYPE_REQUIRED =
      validation("tenantgame.bet_type_required");
  public static final ErrorDescriptor BET_TYPE_UNSUPPORTED =
      validation("tenantgame.bet_type_unsupported");
  public static final ErrorDescriptor BET_TYPE_DUPLICATE =
      validation("tenantgame.bet_type_duplicate");
  public static final ErrorDescriptor BET_OPTION_INVALID =
      validation("tenantgame.bet_option_invalid");
  public static final ErrorDescriptor BET_OPTION_DUPLICATE =
      validation("tenantgame.bet_option_duplicate");
  public static final ErrorDescriptor BET_OPTION_ENABLED_REQUIRED =
      validation("tenantgame.bet_option_enabled_required");
  public static final ErrorDescriptor BET_OPTION_DEFAULT_INVALID =
      validation("tenantgame.bet_option_default_invalid");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        CATALOG_GAME_NOT_FOUND,
        NOT_FOUND,
        DISABLED,
        CATALOG_GAME_INACTIVE,
        DISPLAY_NAME_TOO_LONG,
        DISPLAY_ORDER_INVALID,
        MIN_STAKE_INVALID,
        MAX_STAKE_INVALID,
        STAKE_RANGE_INVALID,
        AVAILABILITY_DAY_INVALID,
        TIME_INVALID,
        BET_TYPE_REQUIRED,
        BET_TYPE_UNSUPPORTED,
        BET_TYPE_DUPLICATE,
        BET_OPTION_INVALID,
        BET_OPTION_DUPLICATE,
        BET_OPTION_ENABLED_REQUIRED,
        BET_OPTION_DEFAULT_INVALID);
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
