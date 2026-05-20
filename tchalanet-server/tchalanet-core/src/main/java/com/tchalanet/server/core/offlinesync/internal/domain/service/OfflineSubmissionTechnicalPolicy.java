package com.tchalanet.server.core.offlinesync.internal.domain.service;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;
import com.tchalanet.server.core.offlinesync.api.model.grant.OfflineGrantStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain policy applying the 15 ordered technical checks of OFFLINESYNC v2.1.
 *
 * <p>Inputs are gathered by the {@code SyncOfflineSalesCommandHandler} and passed in already
 * resolved (POS context, signature verifier, etc.). This class makes the decision only.
 *
 * <p>Order matters: cheap and security-critical checks first; quota check last.
 */
public final class OfflineSubmissionTechnicalPolicy {

    private OfflineSubmissionTechnicalPolicy() {}

    public record Inputs(
        boolean tenantOfflineEnabled,
        boolean planAllowsOffline,
        boolean trustedOperationalContext,
        boolean posContextValidated,
        OfflineGrant grant,
        UUID submissionDeviceId,
        String submissionDevicePublicKey,
        boolean signatureValid,
        OfflineCode code,
        OfflineCodeBatch codeBatch,
        Instant clientSoldAt,
        Instant receivedAt,
        String declaredPayloadHash,
        String recomputedPayloadHash,
        Money submissionStake
    ) {}

    public sealed interface Decision {
        record Accept() implements Decision {}
        record Reject(String code, String reason) implements Decision {}
    }

    public static Decision evaluate(Inputs in) {
        // 1. feature flag tenant
        if (!in.tenantOfflineEnabled())
            return reject("offlinesync.disabled", "Tenant feature flag offlinesync.enabled is off");
        // 2. plan allows offline
        if (!in.planAllowsOffline())
            return reject("offlinesync.plan_forbidden", "Tenant plan does not allow offline sales");
        // 3. trusted operational context
        if (!in.trustedOperationalContext())
            return reject("offlinesync.context_untrusted", "Trusted operational context required");
        // 4. POS context validated
        if (!in.posContextValidated())
            return reject("offlinesync.pos_context_invalid", "POS operational context validation failed");
        // 5. grant exists
        if (in.grant() == null)
            return reject("offlinesync.grant.not_found", "Offline grant not found");
        OfflineGrant grant = in.grant();
        // 6. grant not revoked
        if (grant.status() == OfflineGrantStatus.REVOKED)
            return reject("offlinesync.grant.revoked", "Offline grant has been revoked");
        // 7. clientSoldAt within [validFrom, validUntil)
        if (!grant.isWithinValidity(in.clientSoldAt()))
            return reject("offlinesync.grant.outside_validity",
                "clientSoldAt not in [validFrom, validUntil)");
        // 8. receivedAt <= syncAcceptedUntil
        if (!grant.isWithinSyncWindow(in.receivedAt()))
            return reject("offlinesync.grant.sync_window_closed",
                "receivedAt past syncAcceptedUntil");
        // 9. deviceId and public key match the grant
        if (!grant.deviceId().equals(in.submissionDeviceId())
            || !grant.devicePublicKey().equals(in.submissionDevicePublicKey()))
            return reject("offlinesync.device_mismatch",
                "Submission device does not match grant device");
        // 10. Ed25519 signature valid
        if (!in.signatureValid())
            return reject("offlinesync.signature.invalid", "Ed25519 signature verification failed");
        // 11. code exists in the batch
        if (in.code() == null)
            return reject("offlinesync.code.not_found", "Offline code unknown");
        if (in.codeBatch() == null
            || !in.code().codeBatchId().equals(in.codeBatch().id()))
            return reject("offlinesync.code.batch_mismatch",
                "Offline code does not belong to declared batch");
        // 12. code AVAILABLE then pessimistic lock to RESERVED
        if (in.code().status() != OfflineCodeStatus.AVAILABLE)
            return reject("offlinesync.code.not_available",
                "Offline code is not AVAILABLE (was " + in.code().status() + ")");
        // 13. batch not expired
        if (in.codeBatch().isExpired(in.receivedAt()))
            return reject("offlinesync.code.batch_expired", "Code batch is expired");
        // 14. payloadHash consistency
        if (in.declaredPayloadHash() == null
            || !in.declaredPayloadHash().equals(in.recomputedPayloadHash()))
            return reject("offlinesync.submission.payload_mismatch",
                "Declared payloadHash differs from recomputed");
        // 15. offline quotas not exceeded
        if (!grant.canAccept(in.submissionStake()))
            return reject("offlinesync.grant.quota_exceeded",
                "Grant quota would be exceeded by this submission");

        return new Decision.Accept();
    }

    private static Decision.Reject reject(String code, String reason) {
        return new Decision.Reject(code, reason);
    }
}
