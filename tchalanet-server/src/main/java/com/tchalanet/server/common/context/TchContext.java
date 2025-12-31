package com.tchalanet.server.common.context;

/** Thread-local context used for non-HTTP threads (startup, batch, async). */
public final class TchContext {

  private static final ThreadLocal<TchRequestContext> HOLDER = new ThreadLocal<>();

  private TchContext() {}

  public static void set(TchRequestContext ctx) {
    HOLDER.set(ctx);
  }

  public static TchRequestContext get() {
    return HOLDER.get();
  }

  public static void clear() {
    HOLDER.remove();
  }

  public static boolean hasContext() {
    return HOLDER.get() != null;
  }

  /** Return the current context or null. */
  public static TchRequestContext currentOrNull() {
    return HOLDER.get();
  }
}
