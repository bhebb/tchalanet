package com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Provisions and manages the external identity (Firebase) for a SellerTerminal.
 * Implemented in tchalanet-platform (S4).
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
     * Updates the Firebase password for an existing terminal.
     * Used by the reset-access flow.
     */
    void resetPin(SellerTerminalId id, TenantId tenantId, String newPin);
}
