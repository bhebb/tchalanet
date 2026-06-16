package com.tchalanet.server.platform.identity.internal.persistence;

import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.service.SellerTerminalIdentityResolution;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Stub implementation — always returns empty.
 *
 * <p>The {@code seller_terminal_external_identity} table does not exist yet (deferred to
 * {@code seller-terminal-v0}). This stub satisfies the port contract so the bootstrap
 * filter wiring compiles and the AppUser path is unaffected. Replace with a real JPA
 * repository implementation once the table is created.
 */
// Stub disabled — replaced by SellerTerminalExternalIdentityAdapter (S4)
class StubSellerTerminalExternalIdentityRepository implements SellerTerminalExternalIdentityPort {

    @Override
    public Optional<SellerTerminalIdentityResolution> findByExternalIdentity(
        IdentityProviderType provider,
        String issuer,
        String externalSubject
    ) {
        return Optional.empty();
    }
}
