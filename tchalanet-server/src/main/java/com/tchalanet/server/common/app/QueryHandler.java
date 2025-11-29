package com.tchalanet.server.common.app;

public interface QueryHandler<Q, R> {
  R handle(Q query);
}
