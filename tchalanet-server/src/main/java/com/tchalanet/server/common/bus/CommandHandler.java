package com.tchalanet.server.common.bus;

public interface CommandHandler<C, R> {
  R handle(C command);
}
