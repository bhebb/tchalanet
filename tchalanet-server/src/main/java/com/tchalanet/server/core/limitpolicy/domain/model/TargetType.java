package com.tchalanet.server.core.limitpolicy.domain.model;

/**
 * Enumeration of target types for limit assignments.
 *
 * Defines where a limit assignment is attached:
 * - TENANT: Applies to the entire tenant organization
 * - AGENT: Applies to a specific user/agent
 * - OUTLET: Applies to a specific outlet/location
 * - TERMINAL: Applies to a specific terminal device
 * - GAME: Applies to a specific game
 */
public enum TargetType {
    TENANT, AGENT, OUTLET, TERMINAL, GAME
}
