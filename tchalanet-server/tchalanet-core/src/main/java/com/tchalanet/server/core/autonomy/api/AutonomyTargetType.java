package com.tchalanet.server.core.autonomy.api;

/**
 * Enumeration of limitScopeRef types for autonomy policies.
 *
 * <p>Defines the scope at which an autonomy policy applies: - TENANT: Applies to the entire tenant
 * organization - OUTLET: Applies to a specific outlet/location - TERMINAL: Applies to a specific
 * terminal device - AGENT: Applies to a specific user/agent
 */
public enum AutonomyTargetType {
    TENANT,
    TERMINAL,
    USER,
    OUTLET
}
