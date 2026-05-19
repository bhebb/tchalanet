package com.tchalanet.server.core.offlinesync.internal.domain.model.grant;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.grant.GrantLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.grant.GrantQuota;
import com.tchalanet.server.core.offlinesync.api.model.grant.GrantValidityWindow;
import com.tchalanet.server.core.offlinesync.api.model.grant.OfflineGrantStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * OfflineGrant — Aggregate root.
 *
 * <p>Carries the cryptographic + business limits within which a POS device may produce
 * offline sales. Composed of {@link GrantIdentity}, {@link GrantDevice},
 * {@link GrantValidityWindow}, {@link GrantQuota} and {@link GrantLifecycle}.
 *
 * <p>Optimistic locking is handled by the JPA superclass on the persistence side — the
 * domain does not carry a {@code version} field.
 */
public record OfflineGrant(
    GrantIdentity identity,
    GrantDevice device,
    GrantValidityWindow window,
    GrantQuota quota,
    GrantLifecycle lifecycle
) {

    public OfflineGrant {
        if (identity == null) throw new IllegalArgumentException("identity required");
        if (device == null) throw new IllegalArgumentException("device required");
        if (window == null) throw new IllegalArgumentException("window required");
        if (quota == null) throw new IllegalArgumentException("quota required");
        if (lifecycle == null) throw new IllegalArgumentException("lifecycle required");
    }

    public static OfflineGrant issue(
        OfflineGrantId id, TenantId tenantId, UserId sellerUserId,
        TerminalId terminalId, OutletId outletId, SalesSessionId salesSessionId,
        UUID deviceId, String devicePublicKey, String keyId,
        Instant validFrom, Instant validUntil, Instant syncAcceptedUntil,
        int maxTicketCount, Money maxTotalAmount, Instant issuedAt
    ) {
        return new OfflineGrant(
            new GrantIdentity(id, tenantId, sellerUserId, terminalId, outletId, salesSessionId),
            new GrantDevice(deviceId, devicePublicKey, keyId),
            new GrantValidityWindow(validFrom, validUntil, syncAcceptedUntil),
            GrantQuota.initial(maxTicketCount, maxTotalAmount),
            GrantLifecycle.issued(issuedAt)
        );
    }

    public OfflineGrant revoke(String reason, Instant now) {
        return new OfflineGrant(identity, device, window, quota, lifecycle.revoked(reason, now));
    }

    public OfflineGrant expire() {
        if (lifecycle.status() != OfflineGrantStatus.ACTIVE) return this;
        return new OfflineGrant(identity, device, window, quota, lifecycle.expired());
    }

    /**
     * Increment quota counters when a submission is technically validated.
     * Caller must have already checked {@link #canAccept(Money)}.
     */
    public OfflineGrant recordValidatedTicket(Money stake) {
        if (!quota.canAccept(stake))
            throw new IllegalStateException("grant " + identity.id() + " quota exceeded");
        return new OfflineGrant(identity, device, window, quota.recordValidated(stake), lifecycle);
    }

    public boolean canAccept(Money stake) {
        if (lifecycle.status() != OfflineGrantStatus.ACTIVE) return false;
        return quota.canAccept(stake);
    }

    // Convenience accessors so policies and listeners do not have to reach through.
    public OfflineGrantId id() { return identity.id(); }
    public TenantId tenantId() { return identity.tenantId(); }
    public UUID deviceId() { return device.deviceId(); }
    public String devicePublicKey() { return device.devicePublicKey(); }
    public OfflineGrantStatus status() { return lifecycle.status(); }
    public Instant syncAcceptedUntil() { return window.syncAcceptedUntil(); }

    public boolean isWithinValidity(Instant clientSoldAt) {
        return window.containsForSale(clientSoldAt);
    }

    public boolean isWithinSyncWindow(Instant receivedAt) {
        return window.acceptsSyncAt(receivedAt);
    }
}
