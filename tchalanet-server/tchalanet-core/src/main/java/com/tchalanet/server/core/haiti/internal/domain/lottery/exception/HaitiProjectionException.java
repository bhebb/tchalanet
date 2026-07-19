package com.tchalanet.server.core.haiti.internal.domain.lottery.exception;

/** Raised when a valid external pick cannot be projected with the configured Haiti rules. */
public final class HaitiProjectionException extends RuntimeException {

  public HaitiProjectionException(String reason) {
    super(reason);
  }
}
