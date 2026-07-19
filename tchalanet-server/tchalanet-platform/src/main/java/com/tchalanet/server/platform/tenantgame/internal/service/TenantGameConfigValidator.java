package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.tenantgame.api.error.TenantGameErrorCodes;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TenantGameConfigValidator {

  private static final Set<String> VALID_DAY_CODES =
      Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

  public void validateEnableGame(GameView game) {
    if (!game.active()) {
      throw ProblemRest.of(TenantGameErrorCodes.CATALOG_GAME_INACTIVE);
    }
  }

  public void validateSettings(UpdateTenantGameSettingsRequest req) {
    if (req.getDisplayName() != null && req.getDisplayName().length() > 128) {
      throw ProblemRest.of(TenantGameErrorCodes.DISPLAY_NAME_TOO_LONG);
    }
    if (req.getDisplayOrder() != null && req.getDisplayOrder() < 0) {
      throw ProblemRest.of(TenantGameErrorCodes.DISPLAY_ORDER_INVALID);
    }
    validateStakeRange(req.getMinStake(), req.getMaxStake());
    if (hasText(req.getAvailabilityDays())) {
      validateAvailabilityDays(req.getAvailabilityDays());
    }
    if (hasText(req.getStartLocalTime())) parseTime(req.getStartLocalTime(), "startLocalTime");
    if (hasText(req.getEndLocalTime())) parseTime(req.getEndLocalTime(), "endLocalTime");
  }

  public void validateStakeRange(BigDecimal min, BigDecimal max) {
    if (min != null && min.signum() < 0) {
      throw ProblemRest.of(TenantGameErrorCodes.MIN_STAKE_INVALID);
    }
    if (max != null && max.signum() < 0) {
      throw ProblemRest.of(TenantGameErrorCodes.MAX_STAKE_INVALID);
    }
    if (min != null && max != null && min.compareTo(max) > 0) {
      throw ProblemRest.of(TenantGameErrorCodes.STAKE_RANGE_INVALID);
    }
  }

  private void validateAvailabilityDays(String days) {
    for (String day : days.split(",")) {
      var code = day.trim().toUpperCase();
      if (!VALID_DAY_CODES.contains(code)) {
        throw ProblemRest.of(TenantGameErrorCodes.AVAILABILITY_DAY_INVALID);
      }
    }
  }

  private LocalTime parseTime(String value, String field) {
    try {
      return LocalTime.parse(value);
    } catch (DateTimeParseException e) {
      throw ProblemRest.of(TenantGameErrorCodes.TIME_INVALID, java.util.Map.of(), e);
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
