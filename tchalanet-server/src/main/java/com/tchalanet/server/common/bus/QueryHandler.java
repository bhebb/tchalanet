package com.tchalanet.server.common.bus;

public interface QueryHandler<Q, R> {
  R handle(Q query);
}
