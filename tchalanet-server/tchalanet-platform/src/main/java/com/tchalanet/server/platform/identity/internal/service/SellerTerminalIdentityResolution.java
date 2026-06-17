package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Result of a SellerTerminal external identity lookup during bootstrap.
 * Produced by {@link com.tchalanet.server.platform.identity.internal.persistence.SellerTerminalExternalIdentityPort}.
 */
public record SellerTerminalIdentityResolution(
    SellerTerminalId sellerTerminalId,
    TenantId tenantId,
    TerminalBootstrapStatus status
) {
    public boolean isActive() {
        return status == TerminalBootstrapStatus.ACTIVE;
    }

    public enum TerminalBootstrapStatus {
        ACTIVE,
        BLOCKED,
        DISABLED
    }
}
