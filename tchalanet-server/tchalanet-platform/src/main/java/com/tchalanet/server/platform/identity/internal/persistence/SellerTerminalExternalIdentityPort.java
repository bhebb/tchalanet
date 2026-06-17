package com.tchalanet.server.platform.identity.internal.persistence;

import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.service.SellerTerminalIdentityResolution;

import java.util.Optional;

/**
 * Port for resolving a provider external identity to a provisioned SellerTerminal.
 *
 * <p>The {@code seller_terminal_external_identity} table is deferred to {@code seller-terminal-v0}.
 * This port returns {@code Optional.empty()} until that change lands and a real implementation
 * is wired in.
 *
 * <p>Implementations must not depend on tenant RLS context — this is called before tenant
 * resolution, during identity bootstrap.
 */
public interface SellerTerminalExternalIdentityPort {

    /**
     * Look up a SellerTerminal by its external provider identity.
     *
     * @param provider       identity provider type (e.g. FIREBASE)
     * @param issuer         token issuer URL
     * @param externalSubject provider subject claim (unique per provider+issuer)
     * @return resolved terminal with tenant and status, or empty if not found
     */
    Optional<SellerTerminalIdentityResolution> findByExternalIdentity(
        IdentityProviderType provider,
        String issuer,
        String externalSubject
    );
}
