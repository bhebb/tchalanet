package com.tchalanet.server.core.autonomy.domain.model;

/**
 * Enumeration of user roles that can approve restricted transactions.
 *
 * - OPERATOR: Standard operator role with basic approval permissions
 * - ADMIN: Administrative role with elevated approval permissions
 */
public enum ApprovalRole {
    OPERATOR, ADMIN
}
