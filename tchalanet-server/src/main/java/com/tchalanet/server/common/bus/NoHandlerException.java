package com.tchalanet.server.common.bus;

/**
 * Exception thrown when no handler is registered for a command or query.
 */
public class NoHandlerException extends IllegalStateException {

  public NoHandlerException(String message) {
    super(message);
  }

  public static NoHandlerException forCommand(Class<?> commandType) {
    return new NoHandlerException("No handler registered for command: " + commandType.getName());
  }

  public static NoHandlerException forQuery(Class<?> queryType) {
    return new NoHandlerException("No handler registered for query: " + queryType.getName());
  }
}

