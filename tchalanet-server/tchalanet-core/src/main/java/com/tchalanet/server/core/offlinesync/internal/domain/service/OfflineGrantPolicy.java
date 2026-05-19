package com.tchalanet.server.core.offlinesync.internal.domain.service;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure domain policy. Decides whether an offline grant can be issued / renewed,
 * given tenant policy inputs and the current grant state. No Spring, no I/O.
 */
public final class OfflineGrantPolicy {

    private OfflineGrantPolicy() {}

    public record Inputs(
        boolean offlineEnabled,
        int batchSize,
        Duration validityDuration,
        Duration syncAcceptedExtension,
        int maxTicketCount,
        Money maxTotalAmount
    ) {}

    public sealed interface Decision {
        record Accept(
            Instant validFrom,
            Instant validUntil,
            Instant syncAcceptedUntil,
            int maxTicketCount,
            Money maxTotalAmount
        ) implements Decision {}
        record Reject(String code, String reason) implements Decision {}
    }

    public static Decision evaluateIssue(Inputs policy, Instant now) {
        if (!policy.offlineEnabled()) {
            return new Decision.Reject(
                "offlinesync.grant.offline_disabled",
                "Tenant policy disables offline sales"
            );
        }
        if (policy.batchSize() <= 0)
            return new Decision.Reject("offlinesync.grant.invalid_batch_size", "batchSize must be > 0");
        if (policy.validityDuration().isZero() || policy.validityDuration().isNegative())
            return new Decision.Reject("offlinesync.grant.invalid_validity", "validityDuration must be > 0");

        Instant validFrom = now;
        Instant validUntil = now.plus(policy.validityDuration());
        Instant syncAcceptedUntil = validUntil.plus(policy.syncAcceptedExtension());
        return new Decision.Accept(
            validFrom, validUntil, syncAcceptedUntil,
            policy.maxTicketCount(), policy.maxTotalAmount()
        );
    }

    public static boolean canRenew(OfflineGrant current, Instant now) {
        return current.isWithinValidity(now) && !now.isAfter(current.syncAcceptedUntil());
    }
}
