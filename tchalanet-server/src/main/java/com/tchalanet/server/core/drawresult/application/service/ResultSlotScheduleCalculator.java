package com.tchalanet.server.core.drawresult.application.service;

import com.tchalanet.server.core.drawresult.application.view.PublicNextResultTimeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ResultSlotScheduleCalculator {

    private final Clock clock;

    public PublicNextResultTimeView calculateNextResultTime(
        LocalTime drawTime,
        String timezone,
        boolean slotActive
    ) {
        if (!slotActive) {
            return disabledStatus(drawTime, timezone);
        }

        var zoneId = ZoneId.of(timezone);
        var now = ZonedDateTime.now(clock.withZone(zoneId));
        var todayDrawTime = now.withHour(drawTime.getHour())
            .withMinute(drawTime.getMinute())
            .withSecond(0)
            .withNano(0);

        var expectedAt = now.isBefore(todayDrawTime)
            ? todayDrawTime
            : todayDrawTime.plusDays(1);

        var countdownSeconds = Math.max(0,
            ChronoUnit.SECONDS.between(now, expectedAt)
        );

        return new PublicNextResultTimeView(
            expectedAt.toInstant(),
            expectedAt.toLocalDate(),
            expectedAt.toLocalTime(),
            timezone,
            countdownSeconds,
            "WAITING"
        );
    }

    private PublicNextResultTimeView disabledStatus(LocalTime drawTime, String timezone) {
        var zoneId = ZoneId.of(timezone);
        var now = ZonedDateTime.now(clock.withZone(zoneId));
        var todayDrawTime = now.withHour(drawTime.getHour())
            .withMinute(drawTime.getMinute())
            .withSecond(0)
            .withNano(0);

        return new PublicNextResultTimeView(
            todayDrawTime.toInstant(),
            todayDrawTime.toLocalDate(),
            todayDrawTime.toLocalTime(),
            timezone,
            0,
            "DISABLED"
        );
    }
}
