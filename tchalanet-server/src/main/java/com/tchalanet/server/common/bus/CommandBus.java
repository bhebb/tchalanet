package com.tchalanet.server.common.bus;

public interface CommandBus {
  <R> R send(Command<R> command);
}
