package com.tchalanet.server.core.uslottery.internal.application.port.out;

import com.tchalanet.server.common.types.enums.ResultQuality;

import java.util.List;

public record UsLotteryProviderResult(
    String externalGameCode,
    List<String> main,
    List<String> extras,
    ResultQuality quality,
    UsProviderSourceFlags sourceFlags,
    java.time.Instant occurredAt,
    Object rawPayload) {

    public boolean found() {
        return main != null && !main.isEmpty();
    }
}
