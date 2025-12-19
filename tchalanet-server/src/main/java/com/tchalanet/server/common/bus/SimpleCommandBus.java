package com.tchalanet.server.common.bus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SimpleCommandBus implements CommandBus {

    private final ApplicationContext ctx;

    private final Map<Class<?>, Object> handlers = new HashMap<>();

    @PostConstruct
    void init() {
        // CommandHandler<C,R>
        Map<String, CommandHandler> commandHandlers = ctx.getBeansOfType(CommandHandler.class);
        for (CommandHandler<?, ?> bean : commandHandlers.values()) {
            registerHandler(bean, CommandHandler.class, 0);
        }

        // VoidCommandHandler<C>
        Map<String, VoidCommandHandler> voidHandlers = ctx.getBeansOfType(VoidCommandHandler.class);
        for (VoidCommandHandler<?> bean : voidHandlers.values()) {
            registerHandler(bean, VoidCommandHandler.class, 0);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        Object handler = handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException("No handler for command " + command.getClass().getName());
        }

        if (handler instanceof CommandHandler<?, ?> ch) {
            return (R) ((CommandHandler<Command<R>, R>) ch).handle(command);
        }

        if (handler instanceof VoidCommandHandler<?> vh) {
            ((VoidCommandHandler<Command<Void>>) vh).handle((Command<Void>) command);
            return null;
        }

        throw new IllegalStateException(
            "Invalid handler type for command " + command.getClass().getName() + ": " + handler.getClass().getName());
    }

    private void registerHandler(Object bean, Class<?> genericIfc, int index) {
        Class<?> implClass = AopUtils.getTargetClass(bean);
        Class<?> msgType = resolveGenericParameter(implClass, genericIfc, index);
        if (msgType == null) {
            return;
        }
        Object previous = handlers.putIfAbsent(msgType, bean);
        if (previous != null) {
            throw new IllegalStateException(
                "Multiple handlers found for " + msgType.getName()
                    + ": " + AopUtils.getTargetClass(previous).getName()
                    + " and " + implClass.getName());
        }
    }

    private static Class<?> resolveGenericParameter(Class<?> implClass, Class<?> genericIfc, int index) {
        for (Type t : implClass.getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt) {
                Type raw = pt.getRawType();
                if (raw instanceof Class<?> rc && rc.equals(genericIfc)) {
                    Type arg = pt.getActualTypeArguments()[index];
                    if (arg instanceof Class<?>) return (Class<?>) arg;
                }
            }
        }
        Class<?> sup = implClass.getSuperclass();
        if (sup != null && !sup.equals(Object.class)) return resolveGenericParameter(sup, genericIfc, index);
        return null;
    }
}
