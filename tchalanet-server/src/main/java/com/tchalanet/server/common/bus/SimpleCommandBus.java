package com.tchalanet.server.common.bus;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleCommandBus implements CommandBus {

  private final ApplicationContext ctx;

  private Map<Class<?>, Object> handlers = Map.of();

  @PostConstruct
  @SuppressWarnings({"unchecked", "rawtypes"})
  void init() {
    Map<String, CommandHandler<?, ?>> commandHandlers = (Map) ctx.getBeansOfType(CommandHandler.class);
    Map<String, VoidCommandHandler<?>> voidHandlers = (Map) ctx.getBeansOfType(VoidCommandHandler.class);

    handlers = HandlerRegistry.buildCommandRegistry(commandHandlers, voidHandlers);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R execute(Command<R> command) {
    Objects.requireNonNull(command, "Command must not be null");

    Object handler = handlers.get(command.getClass());
    if (handler == null) {
      throw NoHandlerException.forCommand(command.getClass());
    }

    if (handler instanceof CommandHandler<?, ?> ch) {
      return ((CommandHandler<Command<R>, R>) ch).handle(command);
    }

    if (handler instanceof VoidCommandHandler<?> vh) {
      ((VoidCommandHandler<Command<Void>>) vh).handle((Command<Void>) command);
      return null;
    }

    throw new IllegalStateException(
        "Invalid handler type for command "
            + command.getClass().getName()
            + ": "
            + handler.getClass().getName());
  }
}
