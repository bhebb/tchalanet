package com.tchalanet.server.common.bus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleCommandBus implements CommandBus {

    private final ApplicationContext ctx;
    private final Map<Class<?>, Object> handlers = new HashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() {
        // Discover handlers implementing common.bus.CommandHandler
        Map<String, CommandHandler> busBeans = ctx.getBeansOfType(CommandHandler.class);
        for (CommandHandler bean : busBeans.values()) {
            Class<?> implClass = AopUtils.getTargetClass(bean);
            Class<?> commandType = resolveGenericParameter(implClass, CommandHandler.class, 0);
            if (commandType != null) {
                handlers.put(commandType, bean);
            }
        }

        // Also discover handlers implementing common.app.CommandHandler (project-level)
        Map<String, CommandHandler> appBeans = ctx.getBeansOfType(CommandHandler.class);
        for (var bean : appBeans.values()) {
            Class<?> implClass = AopUtils.getTargetClass(bean);
            Class<?> commandType = resolveGenericParameter(implClass, CommandHandler.class, 0);
            if (commandType != null) {
                handlers.put(commandType, bean);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        Object handler = handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException("No CommandHandler for " + command.getClass());
        }
        // Try bus handler first
        if (handler instanceof CommandHandler ch) {
            return (R) ch.handle(command);
        }
        // Then try app-level handler
        if (handler instanceof CommandHandler appCh) {
            return (R) appCh.handle(command);
        }
        throw new IllegalStateException("Handler for " + command.getClass() + " is not a valid CommandHandler");
    }

    private static Class<?> resolveGenericParameter(Class<?> implClass, Class<?> genericIfc, int index) {
        // Walk class hierarchy and interfaces to find ParameterizedType for genericIfc
        for (Type t : implClass.getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt) {
                Type raw = pt.getRawType();
                if (raw instanceof Class<?> rc && genericIfc.isAssignableFrom(rc)) {
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
