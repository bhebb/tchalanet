package com.tchalanet.server.common.bus.exception;

/**
 * Exception thrown when multiple handlers are registered for the same message class.
 */
public class DuplicateHandlerException extends BusRegistrationException {

  public DuplicateHandlerException(String message) {
    super(message);
  }

  public static DuplicateHandlerException forMessage(
      Class<?> messageType, Class<?> existingHandler, Class<?> newHandler) {
    return new DuplicateHandlerException(
        String.format(
            "Multiple handlers found for message %s: %s and %s",
            messageType.getName(), existingHandler.getName(), newHandler.getName()));
  }
}

