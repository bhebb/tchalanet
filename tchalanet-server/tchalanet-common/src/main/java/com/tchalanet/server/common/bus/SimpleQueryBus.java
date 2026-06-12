package com.tchalanet.server.common.bus;

import com.tchalanet.server.common.bus.exception.NoHandlerException;
import com.tchalanet.server.common.bus.registry.HandlerRegistry;
import java.util.Map;
import java.util.Objects;

import com.tchalanet.server.common.stereotype.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleQueryBus implements QueryBus, SmartInitializingSingleton {

    private final ApplicationContext ctx;

    private volatile Map<Class<?>, QueryHandler<?, ?>> handlers = Map.of();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void afterSingletonsInstantiated() {
        Map<String, QueryHandler<?, ?>> beans =
            (Map) ctx.getBeansOfType(QueryHandler.class, true, true);
        beans.forEach((name, bean) ->
            log.debug(
                "QueryHandler candidate name={} proxyClass={} targetClass={}",
                name,
                bean.getClass().getName(),
                org.springframework.aop.support.AopUtils.getTargetClass(bean).getName()
            )
        );
        handlers = HandlerRegistry.buildRegistry(
            beans,
            HandlerTypeResolver::resolveQueryHandlerMessageType,
            "QueryBus"
        );
        auditUseCaseQueryHandlers(beans);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R ask(Query<R> query) {
        Objects.requireNonNull(query, "Query must not be null");

        var registry = handlers;
        if (registry.isEmpty()) {
            throw new IllegalStateException("QueryBus registry is not initialized yet");
        }

        QueryHandler<Query<R>, R> handler =
            (QueryHandler<Query<R>, R>) registry.get(query.getClass());

        if (handler == null) {
            throw NoHandlerException.forQuery(query.getClass());
        }

        return handler.handle(query);
    }

    private void auditUseCaseQueryHandlers(Map<String, QueryHandler<?, ?>> discovered) {
        var discoveredNames = discovered.keySet();

        ctx.getBeansWithAnnotation(UseCase.class).forEach((name, bean) -> {
            var targetClass = AopUtils.getTargetClass(bean);

            if (QueryHandler.class.isAssignableFrom(targetClass)
                && !discoveredNames.contains(name)) {
                throw new IllegalStateException(
                    "Bean " + name + " (" + targetClass.getName()
                        + ") implements QueryHandler but was not discovered by getBeansOfType(QueryHandler.class)"
                );
            }
        });
    }
}
