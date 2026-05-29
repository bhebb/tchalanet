package com.tchalanet.server.core.pagemodel.api.dynamic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-request context for PageModel dynamic resolution.
 * Memoizes both successful and failed loads so a grouped payload is fetched at most
 * once per request. A failure is re-thrown for every widget that depends on the same
 * source, but the underlying load is not retried.
 */
public final class PageModelResolutionContext {

  private final Map<String, Object> memo = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public <T> T getOrLoad(String key, Supplier<T> loader) {
    Object cached = memo.computeIfAbsent(key, ignored -> {
      try {
        return new Loaded<>(loader.get());
      } catch (RuntimeException e) {
        return new Failed(e);
      }
    });

    if (cached instanceof Failed failed) {
      throw failed.cause();
    }
    return ((Loaded<T>) cached).value();
  }

  private record Loaded<T>(T value) {}
  private record Failed(RuntimeException cause) {}
}
