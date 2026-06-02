package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Component
public class TenantGameConfigValidator {

    private static final Set<String> VALID_DAY_CODES =
        Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    public void validateEnableGame(GameView game) {
        if (!game.active()) {
            throw new IllegalArgumentException(
                "Cannot enable inactive catalog game: " + game.code());
        }
    }

    public void validateSettings(UpdateTenantGameSettingsRequest req) {
        if (req.getDisplayName() != null && req.getDisplayName().length() > 128) {
            throw new IllegalArgumentException("displayName must be <= 128 characters");
        }
        if (req.getDisplayOrder() != null && req.getDisplayOrder() < 0) {
            throw new IllegalArgumentException("displayOrder must be >= 0");
        }
        validateStakes(req.getMinStake(), req.getMaxStake());
        if (req.getAvailabilityDays() != null) {
            validateAvailabilityDays(req.getAvailabilityDays());
        }
        if (req.getStartLocalTime() != null) parseTime(req.getStartLocalTime(), "startLocalTime");
        if (req.getEndLocalTime() != null) parseTime(req.getEndLocalTime(), "endLocalTime");
    }

    private void validateStakes(BigDecimal min, BigDecimal max) {
        if (min != null && min.signum() < 0) {
            throw new IllegalArgumentException("minStake must be >= 0");
        }
        if (max != null && max.signum() < 0) {
            throw new IllegalArgumentException("maxStake must be >= 0");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("minStake must be <= maxStake");
        }
    }

    private void validateAvailabilityDays(String days) {
        for (String day : days.split(",")) {
            var code = day.trim().toUpperCase();
            if (!VALID_DAY_CODES.contains(code)) {
                throw new IllegalArgumentException(
                    "Invalid availability day code: '" + code + "'. Valid: " + VALID_DAY_CODES);
            }
        }
    }

    private LocalTime parseTime(String value, String field) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be a valid time (HH:mm): " + value);
        }
    }
}
