package com.tchalanet.server.common.web.api;

/**
 * Semantic purpose of non-blocking feedback attached to a successful API response.
 *
 * <p>The kind is intentionally independent from severity: a degradation may be a warning, while a
 * business warning can still leave the requested payload complete.
 */
public enum NoticeKind {
  BUSINESS,
  DEGRADATION,
  INFORMATION
}
