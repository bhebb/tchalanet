package com.tchalanet.server.core.autonomy.domain.model;

/**
 * Enumeration of autonomy levels defining the scope of transaction permissions.
 *
 * - NONE: No autonomy - all transactions require approval
 * - PARTIAL: Partial autonomy - some transactions may require approval based on other rules
 * - FULL: Full autonomy - transactions proceed without approval requirements
 */
public enum AutonomyLevel {
    NONE, PARTIAL, FULL
}
