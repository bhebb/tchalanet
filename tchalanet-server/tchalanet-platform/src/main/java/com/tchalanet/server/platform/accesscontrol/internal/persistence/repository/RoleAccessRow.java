package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

/**
 * Projection for one (role, permission) pair of an access snapshot query. {@code permissionCode} is
 * {@code null} for a role that has no granted permissions (left join).
 */
public interface RoleAccessRow {

  String getRoleCode();

  String getPermissionCode();
}
