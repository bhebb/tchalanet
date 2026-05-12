package com.tchalanet.server.common.bus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

/**
 * Internal utility for building immutable handler registries.
 * Discovers handler beans, resolves message types, detects duplicates, and produces an immutable map.
 */
final class HandlerRegistry {

  private static final Logger log = LoggerFactory.getLogger(HandlerRegistry.class);

  private HandlerRegistry() {}

  /**
   * Builds an immutable registry for command and void-command handlers.
   *
   * @param <H> handler type
   * @param handlers map of handler beans
   * @param typeResolver function to resolve message type from handler
   * @param handlerTypeName name for logging (e.g., "CommandHandler")
   * @return immutable map from message class to handler
   */
  static <H> Map<Class<?>, H> buildRegistry(
      Map<String, H> handlers,
      Function<H, Class<?>> typeResolver,
      String handlerTypeName) {

    long startTime = System.currentTimeMillis();
    Map<Class<?>, H> registry = new HashMap<>();

    for (H handler : handlers.values()) {
      Class<?> targetClass = AopUtils.getTargetClass(handler);
      Class<?> messageType;

      try {
        messageType = typeResolver.apply(handler);
      } catch (InvalidHandlerException e) {
        throw new BusRegistrationException(
            "Failed to resolve message type for " + handlerTypeName + " " + targetClass.getName(),
            e);
      }

      H existingHandler = registry.putIfAbsent(messageType, handler);

      if (existingHandler != null) {
        Class<?> existingClass = AopUtils.getTargetClass(existingHandler);
        throw DuplicateHandlerException.forMessage(messageType, existingClass, targetClass);
      }

      if (log.isDebugEnabled()) {
        log.debug("Registered {} {} -> {}", handlerTypeName, messageType.getSimpleName(), targetClass.getSimpleName());
      }
    }

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "{} initialized: handlers={}, initTimeMs={}",
        handlerTypeName,
        registry.size(),
        duration);

    return Map.copyOf(registry);
  }

  /**
   * Builds an immutable registry for command handlers, detecting conflicts with void handlers.
   *
   * @param commandHandlers map of CommandHandler beans
   * @param voidCommandHandlers map of VoidCommandHandler beans
   * @return immutable map from command class to handler
   */
  static Map<Class<?>, Object> buildCommandRegistry(
      Map<String, CommandHandler> commandHandlers,
      Map<String, VoidCommandHandler<?>> voidCommandHandlers) {

    long startTime = System.currentTimeMillis();
    Map<Class<?>, Object> registry = new HashMap<>();

    // Register CommandHandler<C, R>
    for (CommandHandler<?, ?> handler : commandHandlers.values()) {
      Class<?> targetClass = AopUtils.getTargetClass(handler);
      Class<?> messageType = HandlerTypeResolver.resolveCommandHandlerMessageType(handler);

      Object existingHandler = registry.putIfAbsent(messageType, handler);
      if (existingHandler != null) {
        Class<?> existingClass = AopUtils.getTargetClass(existingHandler);
        throw DuplicateHandlerException.forMessage(messageType, existingClass, targetClass);
      }

      if (log.isDebugEnabled()) {
        log.debug("Registered CommandHandler {} -> {}", messageType.getSimpleName(), targetClass.getSimpleName());
      }
    }

    // Register VoidCommandHandler<C>
    for (VoidCommandHandler<?> handler : voidCommandHandlers.values()) {
      Class<?> targetClass = AopUtils.getTargetClass(handler);
      Class<?> messageType = HandlerTypeResolver.resolveVoidCommandHandlerMessageType(handler);

      Object existingHandler = registry.putIfAbsent(messageType, handler);
      if (existingHandler != null) {
        Class<?> existingClass = AopUtils.getTargetClass(existingHandler);
        throw DuplicateHandlerException.forMessage(messageType, existingClass, targetClass);
      }

      if (log.isDebugEnabled()) {
        log.debug("Registered VoidCommandHandler {} -> {}", messageType.getSimpleName(), targetClass.getSimpleName());
      }
    }

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "CommandBus initialized: commandHandlers={}, voidCommandHandlers={}, totalMessages={}, initTimeMs={}",
        commandHandlers.size(),
        voidCommandHandlers.size(),
        registry.size(),
        duration);

    return Map.copyOf(registry);
  }
}

