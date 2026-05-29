package com.tchalanet.server.core.session.internal.application.service.opening;

import com.tchalanet.server.common.types.id.SalesSessionId;

import java.util.Map;
import java.util.Optional;

public record SalesSessionOpeningEligibility(
    boolean canOpen,
    String denialCode,
    String message,
    Optional<SalesSessionId> currentOpenSessionId,
    Map<String, Object> meta
) {

    public static SalesSessionOpeningEligibility allowed() {
        return new SalesSessionOpeningEligibility(
            true,
            null,
            null,
            Optional.empty(),
            Map.of()
        );
    }

    public static SalesSessionOpeningEligibility denied(
        String denialCode,
        String message
    ) {
        return denied(denialCode, message, Optional.empty(), Map.of());
    }

    public static SalesSessionOpeningEligibility denied(
        String denialCode,
        String message,
        Optional<SalesSessionId> currentOpenSessionId,
        Map<String, Object> meta
    ) {
        return new SalesSessionOpeningEligibility(
            false,
            denialCode,
            message,
            currentOpenSessionId == null ? Optional.empty() : currentOpenSessionId,
            meta == null ? Map.of() : Map.copyOf(meta)
        );
    }
}
