package com.tchalanet.server.common.bus;

public interface QueryBus {
    <R> R send(Query<R> query);
}
