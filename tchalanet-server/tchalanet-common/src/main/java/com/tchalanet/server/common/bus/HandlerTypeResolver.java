package com.tchalanet.server.common.bus;

import com.tchalanet.server.common.bus.exception.InvalidHandlerException;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;

/**
 * Resolves the message type from a handler bean using Spring ResolvableType.
 * Supports CommandHandler, VoidCommandHandler, and QueryHandler.
 */
public final class HandlerTypeResolver {

  private HandlerTypeResolver() {}

  /**
   * Resolves the message type from a CommandHandler bean.
   *
   * @param bean the handler bean
   * @return the command message type
   * @throws InvalidHandlerException if the type cannot be resolved or is not concrete
   */
  public static Class<?> resolveCommandHandlerMessageType(CommandHandler<?, ?> bean) {
    return resolveMessageType(bean, CommandHandler.class, 0);
  }

  /**
   * Resolves the message type from a VoidCommandHandler bean.
   *
   * @param bean the handler bean
   * @return the command message type
   * @throws InvalidHandlerException if the type cannot be resolved or is not concrete
   */
  public static Class<?> resolveVoidCommandHandlerMessageType(VoidCommandHandler<?> bean) {
    return resolveMessageType(bean, VoidCommandHandler.class, 0);
  }

  /**
   * Resolves the message type from a QueryHandler bean.
   *
   * @param bean the handler bean
   * @return the query message type
   * @throws InvalidHandlerException if the type cannot be resolved or is not concrete
   */
  public static Class<?> resolveQueryHandlerMessageType(QueryHandler<?, ?> bean) {
    return resolveMessageType(bean, QueryHandler.class, 0);
  }

  private static Class<?> resolveMessageType(
      Object bean, Class<?> handlerInterface, int genericIndex) {
    Class<?> targetClass = AopUtils.getTargetClass(bean);
    ResolvableType resolvableType = ResolvableType.forClass(targetClass).as(handlerInterface);

    if (resolvableType == ResolvableType.NONE) {
      throw InvalidHandlerException.unresolvableMessageType(targetClass, handlerInterface);
    }

    Class<?> messageType = resolvableType.getGeneric(genericIndex).resolve();

    if (messageType == null) {
      throw InvalidHandlerException.unresolvableMessageType(targetClass, handlerInterface);
    }

    if (messageType.isInterface() || messageType.isArray() || messageType.isPrimitive()) {
      throw InvalidHandlerException.nonConcreteMessageType(targetClass, handlerInterface);
    }

    return messageType;
  }
}
