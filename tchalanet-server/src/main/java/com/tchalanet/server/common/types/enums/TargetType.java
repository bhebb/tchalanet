package com.tchalanet.server.common.types.enums;

/**
 * Enumeration of target types for limit assignments.
 *
 * <p>Defines where a limit assignment is attached: - TENANT: Applies to the entire tenant
 * organization - AGENT: Applies to a specific user/agent - OUTLET: Applies to a specific
 * outlet/location - TERMINAL: Applies to a specific terminal device - GAME: Applies to a specific
 * game - ZONE: Applies to a specific geographic zone - RANGE: Applies to a specific numerical range
 * - DRAWCHANNEL: Applies to a specific draw channel
 */
public enum TargetType {
  TENANT,
  AGENT,
  OUTLET,
  TERMINAL,
  GAME,
  ZONE,
  RANGE,
  DRAWCHANNEL
}
