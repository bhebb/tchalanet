package com.tchalanet.server.limitpolicy.domain.model;

/** Defines the scope of a limit policy. */
public enum LimitScope {
  GLOBAL, // Applies to the entire tenant
  GAME, // Applies to a specific game
  TERMINAL, // Applies to a specific terminal
  USER, // Applies to a specific user/agent
  SESSION, // Applies to a specific POS session
  SELECTION // Applies to a specific number selection (e.g., "123" in a Pick-3 game)
}
