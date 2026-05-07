package com.tchalanet.server.common.bus;

/**
 * Exception thrown when bus initialization fails due to handler registration issues.
 */
public class BusRegistrationException extends IllegalStateException {

  public BusRegistrationException(String message) {
    super(message);
  }

  public BusRegistrationException(String message, Throwable cause) {
    super(message, cause);
  }
}

