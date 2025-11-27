package com.tchalanet.server.audit.domain.model;

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
  OTHER
}
