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
public class SimpleQueryBus implements QueryBus {

    private final ApplicationContext ctx;
    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new HashMap<>();

    @PostConstruct
    void init() {
        Map<String, QueryHandler> beans = ctx.getBeansOfType(QueryHandler.class);
        for (QueryHandler<?, ?> bean : beans.values()) {
            Class<?> implClass = AopUtils.getTargetClass(bean);
            Class<?> queryType = resolveGenericParameter(implClass, QueryHandler.class, 0);
            if (queryType == null) continue;

            QueryHandler<?, ?> previous = handlers.putIfAbsent(queryType, bean);
            if (previous != null) {
                throw new IllegalStateException(
                    "Multiple QueryHandlers found for " + queryType.getName()
                        + ": " + AopUtils.getTargetClass(previous).getName()
                        + " and " + implClass.getName());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalStateException("No QueryHandler for " + query.getClass().getName());
        }
        return handler.handle(query);
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
