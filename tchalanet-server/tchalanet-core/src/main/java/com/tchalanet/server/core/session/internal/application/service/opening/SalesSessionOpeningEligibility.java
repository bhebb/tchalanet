package com.tchalanet.server.core.session.internal.application.service.opening;

import com.tchalanet.server.common.types.id.SalesSessionId;

import java.util.Optional;

public record SalesSessionOpeningEligibility(
    boolean canOpen,
    String denialCode,
    String message,
    SalesSessionOpeningDenialKind denialKind,
    Optional<SalesSessionId> currentOpenSessionId
) {

    public static SalesSessionOpeningEligibility allowed() {
        return new SalesSessionOpeningEligibility(
            true,
            null,
            null,
            null,
            Optional.empty()
        );
    }

    public static SalesSessionOpeningEligibility denied(
        String denialCode,
        String message,
        SalesSessionOpeningDenialKind kind
    ) {
        return new SalesSessionOpeningEligibility(
            false,
            denialCode,
            message,
            kind,
            Optional.empty()
        );
    }

    public static SalesSessionOpeningEligibility denied(
        String denialCode,
        String message,
        SalesSessionOpeningDenialKind kind,
        Optional<SalesSessionId> currentOpenSessionId
    ) {
        return new SalesSessionOpeningEligibility(
            false,
            denialCode,
            message,
            kind,
            currentOpenSessionId == null ? Optional.empty() : currentOpenSessionId
        );
    }
}
