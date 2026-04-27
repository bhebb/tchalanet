package com.tchalanet.server.common.bus;

public interface VoidCommandHandler<C> {
  void handle(C command);
}
