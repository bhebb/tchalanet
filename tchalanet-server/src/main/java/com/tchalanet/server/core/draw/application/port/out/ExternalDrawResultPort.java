package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.core.draw.domain.model.DrawSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public interface ExternalDrawResultPort {

    ExternalDrawResult fetchExternalResult(DrawExternalQuery query);

    record DrawExternalQuery(DrawSource source, ZonedDateTime drawDate) {
    }

    record ExternalDrawResult(
        String channelCode,
        LocalDate drawDate,
        List<String> numbers,
        List<String> numbersExtra,
        Instant occurredAt,
        String rawPayload) {
    }
}
