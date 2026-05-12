package com.tchalanet.server.core.haiti.internal.domain.tchala.exception;

import com.tchalanet.server.common.web.error.NotFoundException;

/**
 * Specific exception when a Tchala entry cannot be found. Extends common NotFoundException to allow
 * centralized handling.
 */
public class TchalaEntryNotFoundException extends NotFoundException {
  public TchalaEntryNotFoundException(String message) {
    super(message);
  }

  public TchalaEntryNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
