package com.tchalanet.server.common.bus;

public interface QueryBus {
  <R> R ask(Query<R> query);
}
