package com.tchalanet.server.core.terminal.internal.application.service;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import java.time.Instant;

public final class TerminalSignaturePayloadCanonicalizerV1 {

    private TerminalSignaturePayloadCanonicalizerV1() {}

    /**
     * Builds the canonical newline-separated payload that the POS signs and the backend verifies.
     *
     * <p>V1 null-to-empty convention: {@code bodyHash}, {@code outletId}, and {@code sessionId}
     * map null to {@code ""}. POS and backend MUST agree on the same representation — do not mix
     * null and {@code ""} for the same field across implementations. This convention is documented
     * in {@code follow-up-mobile.md}.
     *
     * <p>Field order is frozen for V1. Introduce a V2 canonicalizer to add/reorder fields.
     */
    public static String canonicalize(
        TerminalProofPurpose purpose,
        String method,
        String path,
        String bodyHash,
        TerminalId terminalId,
        TerminalBindingId bindingId,
        OutletId outletId,
        SalesSessionId sessionId,
        String nonce,
        Instant signedAt
    ) {
        return String.join("\n",
            purpose.name(),
            method.toUpperCase(),
            path,
            bodyHash != null ? bodyHash : "",
            terminalId.value().toString(),
            bindingId.value().toString(),
            outletId != null ? outletId.value().toString() : "",
            sessionId != null ? sessionId.value().toString() : "",
            nonce,
            signedAt.toString()
        );
    }
}
