package com.tchalanet.server.common.bus;

/**
 * Dispatches write-intent commands to their unique handler.
 *
 * Canonical language:
 * - CommandBus executes a command.
 * - CommandHandler handles a command.
 */
public interface CommandBus {

  <R> R execute(Command<R> command);

  /**
   * Temporary migration bridge.
   *
   * @deprecated use {@link #execute(Command)}.
   */
  @Deprecated(forRemoval = true)
  default <R> R send(Command<R> command) {
    return execute(command);
  }

  /**
   * Temporary migration bridge if older code used bus.handle(...).
   *
   * @deprecated use {@link #execute(Command)}.
   */
  @Deprecated(forRemoval = true)
  default <R> R handle(Command<R> command) {
    return execute(command);
  }
}
