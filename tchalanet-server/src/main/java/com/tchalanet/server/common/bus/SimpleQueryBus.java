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
public class SimpleQueryBus implements QueryBus {

    private final ApplicationContext ctx;
    private final Map<Class<?>, Object> handlers = new HashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() {
        // Discover handlers implementing common.bus.QueryHandler
        Map<String, QueryHandler> busBeans = ctx.getBeansOfType(QueryHandler.class);
        for (QueryHandler bean : busBeans.values()) {
            Class<?> implClass = AopUtils.getTargetClass(bean);
            Class<?> queryType = resolveGenericParameter(implClass, QueryHandler.class, 0);
            if (queryType != null) {
                handlers.put(queryType, bean);
            }
        }

        // Also discover handlers implementing common.app.QueryHandler (project-level)
        Map<String, QueryHandler> appBeans = ctx.getBeansOfType(QueryHandler.class);
        for (var bean : appBeans.values()) {
            Class<?> implClass = AopUtils.getTargetClass(bean);
            Class<?> queryType = resolveGenericParameter(implClass, QueryHandler.class, 0);
            if (queryType != null) {
                handlers.put(queryType, bean);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        Object handler = handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalStateException("No QueryHandler for " + query.getClass());
        }
        if (handler instanceof QueryHandler qh) {
            return (R) qh.handle(query);
        }
        if (handler instanceof QueryHandler appQh) {
            return (R) appQh.handle(query);
        }
        throw new IllegalStateException("Handler for " + query.getClass() + " is not a valid QueryHandler");
    }

    private static Class<?> resolveGenericParameter(Class<?> implClass, Class<?> genericIfc, int index) {
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
