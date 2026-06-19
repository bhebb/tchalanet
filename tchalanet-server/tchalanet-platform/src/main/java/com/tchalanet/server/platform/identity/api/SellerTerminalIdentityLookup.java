package com.tchalanet.server.platform.identity.api;

import com.tchalanet.server.platform.identity.api.model.SellerTerminalIdentityBootstrapView;

import java.util.Optional;

/**
 * Public SPI used during identity bootstrap to resolve a POS external identity.
 *
 * <p>Implementations run before tenant request context is bound, so they must not depend on tenant
 * RLS context.
 */
public interface SellerTerminalIdentityLookup {

    Optional<SellerTerminalIdentityBootstrapView> findByExternalIdentity(
        IdentityProviderType provider,
        String issuer,
        String externalSubject
    );
}
