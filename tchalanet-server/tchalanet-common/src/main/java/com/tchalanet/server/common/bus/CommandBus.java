package com.tchalanet.server.common.bus;

public interface CommandBus {
  <R> R execute(Command<R> command);
}
