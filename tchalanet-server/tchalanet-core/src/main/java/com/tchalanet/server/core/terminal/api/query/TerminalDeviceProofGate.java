package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.error.ProblemRest;
import java.time.Instant;
import java.util.UUID;

/**
 * Controller-layer utility for device proof verification. Controllers extract the
 * five proof headers via @RequestHeader and pass them here along with the operational
 * context for outletId/sessionId resolution.
 *
 * <p>{@code bodyHash} is null in V1 (the canonicalizer substitutes {@code ""}). Pass a
 * precomputed SHA-256 hex of the request body once body-hash verification is enabled.
 */
public final class TerminalDeviceProofGate {

    public static final String HEADER_TERMINAL_ID = "X-Terminal-Id";
    public static final String HEADER_BINDING_ID  = "X-Terminal-Binding-Id";
    public static final String HEADER_NONCE       = "X-Terminal-Nonce";
    public static final String HEADER_SIGNED_AT   = "X-Terminal-Signed-At";
    public static final String HEADER_SIGNATURE   = "X-Terminal-Signature";

    private TerminalDeviceProofGate() {}

    public static void verify(
        QueryBus queryBus,
        TenantId tenantId,
        String terminalIdHeader,
        String bindingIdHeader,
        TerminalProofPurpose purpose,
        String method,
        String path,
        String bodyHash,
        OperationalContextHint op,
        String nonce,
        String signedAtHeader,
        String signature
    ) {
        TerminalId terminalId;
        TerminalBindingId bindingId;
        Instant signedAt;
        try {
            terminalId = TerminalId.of(UUID.fromString(terminalIdHeader));
            bindingId  = TerminalBindingId.of(UUID.fromString(bindingIdHeader));
            signedAt   = Instant.parse(signedAtHeader);
        } catch (Exception e) {
            throw ProblemRest.forbidden("terminal.device_proof_invalid_headers");
        }

        var query = new VerifyTerminalDeviceProofQuery(
            tenantId, terminalId, bindingId, purpose,
            method, path, bodyHash,
            op != null ? op.outletId() : null,
            op != null ? op.salesSessionId() : null,
            nonce, signedAt, signature
        );

        var result = queryBus.ask(query);
        if (result instanceof VerifyTerminalDeviceProofResult.Rejected rejected) {
            throw ProblemRest.forbidden(rejected.code());
        }
    }
}
