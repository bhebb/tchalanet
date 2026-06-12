package com.tchalanet.server.common.bus;

import com.tchalanet.server.common.bus.exception.NoHandlerException;
import com.tchalanet.server.common.bus.registry.HandlerRegistry;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleCommandBus implements CommandBus, SmartInitializingSingleton {

    private final ApplicationContext ctx;

    private volatile Map<Class<?>, Object> handlers = Map.of();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void afterSingletonsInstantiated() {
        Map<String, CommandHandler> commandHandlers =
            (Map) ctx.getBeansOfType(CommandHandler.class, true, true);
        commandHandlers.forEach((name, bean) ->
            log.debug(
                "QueryHandler candidate name={} proxyClass={} targetClass={}",
                name,
                bean.getClass().getName(),
                org.springframework.aop.support.AopUtils.getTargetClass(bean).getName()
            )
        );
        Map<String, VoidCommandHandler<?>> voidHandlers =
            (Map) ctx.getBeansOfType(VoidCommandHandler.class, true, true);

        handlers = HandlerRegistry.buildCommandRegistry(commandHandlers, voidHandlers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(Command<R> command) {
        Objects.requireNonNull(command, "Command must not be null");

        var registry = handlers;
        if (registry.isEmpty()) {
            throw new IllegalStateException("CommandBus registry is not initialized yet");
        }

        Object handler = registry.get(command.getClass());
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
