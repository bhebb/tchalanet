package com.tchalanet.server.core.drawresult.application.port.out.external;

import com.tchalanet.server.common.types.enums.ResultQuality;

import java.time.Instant;
import java.util.List;

public record ExternalResultItem(
    String gameCode,
    List<String> main,
    List<String> extras,
    ResultQuality quality,
    ExternalSourceFlags sourceFlags,
    Instant occurredAt,
    Object rawPayload
) {
    public ExternalResultItem {
        main = main == null ? List.of() : List.copyOf(main);
        extras = extras == null ? List.of() : List.copyOf(extras);
        quality = quality == null ? ResultQuality.SUSPECT : quality;
    }

    public boolean found() {
        return !main.isEmpty();
    }
}
