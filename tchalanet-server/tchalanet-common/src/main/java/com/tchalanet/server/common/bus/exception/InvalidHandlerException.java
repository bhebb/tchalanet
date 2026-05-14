package com.tchalanet.server.common.bus.exception;

/**
 * Exception thrown when a handler is invalid or cannot be used.
 */
public class InvalidHandlerException extends BusRegistrationException {

  public InvalidHandlerException(String message) {
    super(message);
  }

  public InvalidHandlerException(String message, Throwable cause) {
    super(message, cause);
  }

  public static InvalidHandlerException unresolvableMessageType(Class<?> handlerClass, Class<?> handlerInterface) {
    return new InvalidHandlerException(
        String.format(
            "Cannot resolve message type for handler %s implementing %s. "
                + "Ensure the handler directly implements the interface with concrete type parameters.",
            handlerClass.getName(), handlerInterface.getSimpleName()));
  }

  public static InvalidHandlerException nonConcreteMessageType(Class<?> handlerClass, Class<?> handlerInterface) {
    return new InvalidHandlerException(
        String.format(
            "Resolved message type for handler %s implementing %s is not a concrete class",
            handlerClass.getName(), handlerInterface.getSimpleName()));
  }
}

