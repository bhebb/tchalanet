package com.tchalanet.server.core.sellerterminal.internal.application.port.out;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Provisions and manages the external identity (Firebase) for a SellerTerminal.
 * Implemented by the seller-terminal core infrastructure during V0 migration.
 */
public interface SellerTerminalIdentityProvisionPort {

    /**
     * Creates a Firebase user for the terminal and records the external identity link.
     * Called inside the CreateSellerTerminal transaction — rolls back on failure.
     */
    void provision(
        SellerTerminalId id,
        TenantId tenantId,
        String terminalCode,
        String displayName,
        String initialPin
    );

    /**
     * Returns true if this terminal has an external (Firebase) identity bound.
     */
    boolean hasExternalIdentity(SellerTerminalId id);

    /**
     * Updates the Firebase password for an existing terminal.
     * Used by the reset-access flow.
     */
    void resetPin(SellerTerminalId id, TenantId tenantId, String newPin);
}
