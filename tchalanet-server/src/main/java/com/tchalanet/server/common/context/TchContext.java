package com.tchalanet.server.common.context;

import java.util.function.Supplier;

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

  public static TchRequestContext currentOrThrow() {
    var ctx = currentOrNull();
    if (ctx == null) {
      throw new IllegalStateException("Missing TchRequestContext");
    }
    return ctx;
  }

  public static void withContext(TchRequestContext ctx, Runnable work) {
    TchContextScope.runWithContext(ctx, work);
  }

  public static <T> T withContextResult(TchRequestContext ctx, Supplier<T> work) {
    return TchContextScope.runWithContextResult(ctx, work);
  }
}
