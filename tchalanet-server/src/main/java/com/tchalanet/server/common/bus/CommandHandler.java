package com.tchalanet.server.common.bus;

public interface CommandHandler<C extends Command<R>, R> {
  R handle(C command);
}
