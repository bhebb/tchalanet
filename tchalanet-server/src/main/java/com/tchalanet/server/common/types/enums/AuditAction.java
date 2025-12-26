package com.tchalanet.server.common.types.id;

public enum AuditAction {
  CREATE,
  UPDATE,
  DELETE,
  SOFT_DELETE,
  RESTORE,
  STATE_CHANGE,
  PAY,
  LOGIN,
  LOGOUT,
  OVERRIDE_TENANT,
  CACHE_CLEAR,
  SETTLE,
  ARCHIVE,
  OTHER
}
