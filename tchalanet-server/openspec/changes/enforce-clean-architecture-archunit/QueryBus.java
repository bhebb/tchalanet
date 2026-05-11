package com.tchalanet.server.common.bus;

/**
 * Dispatches read-only queries to their unique handler.
 *
 * Canonical language:
 * - QueryBus asks a query.
 * - QueryHandler handles a query.
 */
public interface QueryBus {

  <R> R ask(Query<R> query);

  /**
   * Temporary migration bridge.
   *
   * @deprecated use {@link #ask(Query)}.
   */
  @Deprecated(forRemoval = true)
  default <R> R send(Query<R> query) {
    return ask(query);
  }

  /**
   * Temporary migration bridge if older code used bus.handle(...).
   *
   * @deprecated use {@link #ask(Query)}.
   */
  @Deprecated(forRemoval = true)
  default <R> R handle(Query<R> query) {
    return ask(query);
  }
}
