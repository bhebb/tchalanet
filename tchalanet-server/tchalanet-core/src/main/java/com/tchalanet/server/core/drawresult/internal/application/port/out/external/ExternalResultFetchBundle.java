package com.tchalanet.server.core.drawresult.internal.application.port.out.external;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record ExternalResultFetchBundle(
    String provider,
    LocalDate drawDate,
    LocalTime drawTime,
    ZoneId timezone,
    List<ExternalResultItem> results,
    Object rawPayload
) {

    public static ExternalResultFetchBundle empty(
        String provider,
        ExternalResultsFetchPort.ExternalResultFetchQuery query
    ) {
        return new ExternalResultFetchBundle(
            provider,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.of(),
            null
        );
    }

    public boolean hasAnyResult() {
        return results != null && results.stream().anyMatch(ExternalResultItem::found);
    }

    public ExternalResultItem findByGameCode(String gameCode) {
        if (gameCode == null || gameCode.isBlank() || results == null) {
            return null;
        }

        return results.stream()
            .filter(r -> gameCode.equalsIgnoreCase(r.gameCode()))
            .findFirst()
            .orElse(null);
    }
}
