package com.tchalanet.server.common.web.error;

/** Client families that may receive an externally visible error or notice code. */
public enum ErrorAudience {
  WEB_PUBLIC,
  WEB_ADMIN,
  WEB_PLATFORM,
  MOBILE
}
