package com.tchalanet.server.core.drawresult.application.service;

import com.tchalanet.server.core.drawresult.application.port.out.external.ExternalResultItem;

import java.time.Instant;

public record ResolvedExternalResults(
    ExternalResultItem pick3,
    ExternalResultItem pick4,
    Object rawPayload
) {

    public static ResolvedExternalResults of(
        ExternalResultItem pick3,
        ExternalResultItem pick4,
        Object rawPayload
    ) {
        var bundle = new ResolvedExternalResults(pick3, pick4, rawPayload);
        return bundle.isEmpty() ? empty() : bundle;
    }

    public static ResolvedExternalResults empty() {
        return new ResolvedExternalResults(null, null, null);
    }

    public boolean hasPick3() {
        return isValid(pick3);
    }

    public boolean hasPick4() {
        return isValid(pick4);
    }

    public boolean hasAnyResult() {
        return hasPick3() || hasPick4();
    }

    public boolean isEmpty() {
        return !hasAnyResult();
    }

    private static boolean isValid(ExternalResultItem r) {
        return r != null && r.found();
    }

    public Instant firstOccurredAt() {
        if (hasPick3()) return pick3.occurredAt();
        if (hasPick4()) return pick4.occurredAt();
        return null;
    }
}
