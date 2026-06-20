package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import java.util.UUID;

/**
 * Projection for the DB-backed global access snapshot query.
 */
public interface UserAccessRow {

  UUID getUserId();

  UUID getTenantId();

  String getTenantCode();

  String getTenantName();

  String getTenantStatus();

  String getScope();

  String getRoleCode();

  String getPermissionCode();

  UUID getSellerTerminalId();

  String getTerminalCode();

  String getSellerTerminalStatus();
}
