package com.tchalanet.server.common.types.enums;

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
  LIST,  // v1 decision: audit read-many operations
  OTHER
}
