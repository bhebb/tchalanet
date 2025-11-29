package com.tchalanet.server.common.app;

public interface VoidCommandHandler<C> {
  void handle(C command);
}
