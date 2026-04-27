/**
 * Autonomy Domain.
 *
 * <p>This domain manages autonomy policies that determine the level of approval required for
 * transactions based on user roles and organizational hierarchy.
 *
 * <p>Key components: - AutonomyPolicyRuleRule: Domain entity representing an autonomy rule for a
 * target (TENANT, OUTLET, TERMINAL, AGENT) - AutonomyResolver: Service to resolve the applicable
 * autonomy policy for a given context - AutonomyLevel: Enumeration of autonomy levels (NONE,
 * PARTIAL, FULL) - ApprovalRole: Enumeration of roles that can approve transactions (OPERATOR,
 * ADMIN)
 */
package com.tchalanet.server.core.autonomy.domain;
