package com.tchalanet.server.core.drawresult.internal.application.port.out.external;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

public interface ExternalResultsFetchPort {

    ExternalResultFetchBundle fetchProviderResults(ExternalResultFetchQuery query);

    record ExternalResultFetchQuery(
        String provider,
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId timezone,
        Set<String> gameCodes,
        boolean force,
        boolean includeRaw,
        Instant requestedAt
    ) {
    }
}
