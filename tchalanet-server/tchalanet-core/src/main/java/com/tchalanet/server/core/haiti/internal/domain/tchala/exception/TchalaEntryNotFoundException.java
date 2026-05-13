package com.tchalanet.server.core.haiti.internal.domain.tchala.exception;

/** Specific exception when a Tchala entry cannot be found. */
public class TchalaEntryNotFoundException extends RuntimeException {
  public TchalaEntryNotFoundException(String message) {
    super(message);
  }

  public TchalaEntryNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
