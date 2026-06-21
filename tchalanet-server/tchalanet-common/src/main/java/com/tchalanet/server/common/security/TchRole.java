package com.tchalanet.server.common.security;

/**
 * Canonical runtime roles carried by TchRequestContext.
 *
 * <p>This enum is a context/security compatibility primitive.
 *
 * <p>Role assignment, provider mapping, persistence and permission evaluation
 * belong to platform accesscontrol / identity.
 *
 * <p>New authorization should prefer:
 * <ul>
 *   <li>{@code TchActorType} for actor kind: APP_USER, SELLER_TERMINAL, SYSTEM;</li>
 *   <li>{@code roleCodes: Set<String>} for effective role codes;</li>
 *   <li>{@code permissionKeys: Set<String>} for effective permissions.</li>
 * </ul>
 *
 * <p>{@code CASHIER} and {@code OPERATOR} are legacy roles kept during migration.
 * SellerTerminal is not represented as a role; it is represented by
 * {@code TchActorType.SELLER_TERMINAL}.
 */
public enum TchRole {
    SUPER_ADMIN,
    TENANT_OWNER,
    TENANT_ADMIN,

    /**
     * Legacy role. Retired — SellerTerminal is represented by TchActorType.SELLER_TERMINAL, not a role.
     * @deprecated will be removed after web/mobile guards are updated
     */
    @Deprecated(forRemoval = true)
    CASHIER,

    /**
     * Legacy role kept for compatibility. Do not use for new SellerTerminal authorization.
     */
    OPERATOR,

    /**
     * Batch/scheduler/system actor role compatibility.
     */
    SYSTEM
}
