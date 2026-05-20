package com.tchalanet.server.core.autonomy.internal.domain.model;

/**
 * Enumeration of user roles that can approve restricted transactions.
 *
 * <p>- OPERATOR: Standard operator role with basic approval permissions - ADMIN: Administrative
 * role with elevated approval permissions
 */
public enum ApprovalRole {
  OPERATOR,
  ADMIN
}
