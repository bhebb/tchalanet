package com.tchalanet.server.common.context;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

/**
 * Temporary HTTP pipeline input produced by IdentityBootstrapFilter.
 *
 * <p>Invariants:
 * <ul>
 *   <li>APP_USER: appUserId required, sellerTerminalId null, tenantId may be null until access resolution.</li>
 *   <li>SELLER_TERMINAL: sellerTerminalId required, tenantId required, appUserId null.</li>
 *   <li>SYSTEM: not produced by HTTP identity bootstrap.</li>
 * </ul>
 *
 * <p>Stored as request attribute {@link TchContextRequestAttributes#BOOTSTRAPPED_ACTOR}.
 * Consumed by AccessResolutionFilter; not readable by application code.
 */
public record BootstrappedActor(
    TchActorType actorType,
    UserId appUserId,
    SellerTerminalId sellerTerminalId,
    TenantId tenantId,
    String provider,
    String issuer,
    String externalSubject
) {
    public static BootstrappedActor appUser(
        UserId appUserId,
        String provider,
        String issuer,
        String externalSubject
    ) {
        return new BootstrappedActor(
            TchActorType.APP_USER, appUserId, null, null, provider, issuer, externalSubject);
    }

    public static BootstrappedActor sellerTerminal(
        SellerTerminalId sellerTerminalId,
        TenantId tenantId,
        String provider,
        String issuer,
        String externalSubject
    ) {
        return new BootstrappedActor(
            TchActorType.SELLER_TERMINAL, null, sellerTerminalId, tenantId,
            provider, issuer, externalSubject);
    }

    public boolean isAppUser() {
        return actorType == TchActorType.APP_USER;
    }

    public boolean isSellerTerminal() {
        return actorType == TchActorType.SELLER_TERMINAL;
    }
}
