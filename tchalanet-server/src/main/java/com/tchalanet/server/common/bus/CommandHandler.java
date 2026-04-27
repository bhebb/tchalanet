package com.tchalanet.server.common.bus;

import jakarta.validation.Valid;

public interface CommandHandler<C extends Command<R>, R> {
  R handle(@Valid C command);
}
