package com.tchalanet.server.common.context;

public final class TchRequestContextHolder {
  private static final ThreadLocal<TchRequestContext> CTX = new ThreadLocal<>();

  public static TchRequestContext getCurrentContext() {
    return CTX.get();
  }

  public static void setCurrentContext(TchRequestContext ctx) {
    CTX.set(ctx);
  }

  public static void clear() {
    CTX.remove();
  }
}
