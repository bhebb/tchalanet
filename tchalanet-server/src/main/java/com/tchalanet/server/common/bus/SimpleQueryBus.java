package com.tchalanet.server.common.bus;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleQueryBus implements QueryBus {

  private final ApplicationContext ctx;

  private Map<Class<?>, QueryHandler<?, ?>> handlers = Map.of();

  @PostConstruct
  @SuppressWarnings({"unchecked", "rawtypes"})
  void init() {
    Map<String, QueryHandler<?, ?>> beans = (Map) ctx.getBeansOfType(QueryHandler.class);
    handlers = HandlerRegistry.buildRegistry(
        beans,
        HandlerTypeResolver::resolveQueryHandlerMessageType,
        "QueryBus");
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R ask(Query<R> query) {
    Objects.requireNonNull(query, "Query must not be null");

    QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) handlers.get(query.getClass());
    if (handler == null) {
      throw NoHandlerException.forQuery(query.getClass());
    }
    return handler.handle(query);
  }
}
