package com.tchalanet.server.common.app;

public interface CommandHandler<C, R> {
  R handle(C command);
}
