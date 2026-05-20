package com.tchalanet.server.common.security;

/**
 * Canonical runtime roles carried by TchRequestContext.
 *
 * <p>This enum is a context/security primitive only.
 * Role assignment, Keycloak mapping, persistence and permission evaluation
 * belong to platform accesscontrol / identity.
 */
public enum TchRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    OPERATOR,
    CASHIER,
    SYSTEM
}
