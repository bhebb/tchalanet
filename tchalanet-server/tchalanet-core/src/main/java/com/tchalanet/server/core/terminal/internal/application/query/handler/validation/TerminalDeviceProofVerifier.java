package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.crypto.SignatureVerifier;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import com.tchalanet.server.core.terminal.api.query.VerifyTerminalDeviceProofQuery;
import com.tchalanet.server.core.terminal.api.query.VerifyTerminalDeviceProofResult;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.service.TerminalNonceReplayGuard;
import com.tchalanet.server.core.terminal.internal.application.service.TerminalSignaturePayloadCanonicalizerV1;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalPublicKeyAlgorithm;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class TerminalDeviceProofVerifier
    implements QueryHandler<VerifyTerminalDeviceProofQuery, VerifyTerminalDeviceProofResult> {

    private final TerminalReaderPort terminalReader;
    private final TerminalDeviceBindingReaderPort bindingReader;
    private final TerminalNonceReplayGuard nonceReplayGuard;
    private final SignatureVerifier signatureVerifier;
    private final Clock clock;

    @Override
    public VerifyTerminalDeviceProofResult handle(VerifyTerminalDeviceProofQuery query) {
        var now = Instant.now(clock);

        var terminalOpt = terminalReader.findById(query.tenantId(), query.terminalId());
        if (terminalOpt.isEmpty()) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.not_found");
        }
        var terminal = terminalOpt.get();

        if (terminal.locked()) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.locked");
        }

        var bindingOpt = bindingReader.findById(query.tenantId(), query.bindingId());
        if (bindingOpt.isEmpty()) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.binding_not_found");
        }
        var binding = bindingOpt.get().expireIfDue(now);

        if (!binding.terminalId().equals(query.terminalId())) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.binding_terminal_mismatch");
        }
        if (!binding.activeAt(now)) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.binding_inactive");
        }
        if (binding.bindingPublicKey() == null) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.binding_no_public_key");
        }
        if (binding.publicKeyAlgorithm() != TerminalPublicKeyAlgorithm.ED25519) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.unsupported_algorithm");
        }

        if (!nonceReplayGuard.validateAndRecord(
            query.tenantId(), query.bindingId(), query.purpose(), query.nonce(), query.signedAt(), now)) {
            return rejected(query.purpose(), query.signedAt(), now);
        }

        var canonical = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            query.purpose(), query.method(), query.path(), query.bodyHash(),
            query.terminalId(), query.bindingId(),
            query.outletId(), query.sessionId(),
            query.nonce(), query.signedAt()
        );

        byte[] spkiBytes;
        byte[] signatureBytes;
        try {
            spkiBytes    = Base64.getDecoder().decode(binding.bindingPublicKey());
            signatureBytes = Base64.getUrlDecoder().decode(query.signature());
        } catch (IllegalArgumentException e) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.device_proof_encoding_error");
        }

        if (!signatureVerifier.verify(spkiBytes, canonical.getBytes(StandardCharsets.UTF_8), signatureBytes)) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.device_proof_invalid_signature");
        }

        return new VerifyTerminalDeviceProofResult.Trusted(
            terminal.id(), binding.id(),
            terminal.outletId(), terminal.assignedUserId(),
            binding.bindingType()
        );
    }

    private static VerifyTerminalDeviceProofResult.Rejected rejected(
        TerminalProofPurpose purpose, Instant signedAt, Instant now) {
        var delta = java.time.Duration.between(signedAt, now).abs();
        if (delta.compareTo(java.time.Duration.ofMinutes(5)) > 0) {
            return new VerifyTerminalDeviceProofResult.Rejected("terminal.device_proof_expired_timestamp");
        }
        return new VerifyTerminalDeviceProofResult.Rejected("terminal.device_replay_detected");
    }
}
