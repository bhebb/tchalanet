package com.tchalanet.server.core.autonomy.application.port.out;

import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;

import java.time.Instant;

/**
 * Output port for writing autonomy policy rules to the persistence layer.
 * <p>
 * This port follows the hexagonal architecture pattern, allowing the core domain
 * to remain independent of infrastructure concerns. Implementations are provided
 * by adapters in the infrastructure layer.
 * </p>
 */
public interface AutonomyRuleWriterPort {

    /**
     * Persists an autonomy policy rule.
     * <p>
     * If the policy rule does not exist, it will be created.
     * If it already exists, it will be updated.
     * </p>
     *
     * @param policy the autonomy policy rule to save
     * @return the saved autonomy policy rule with any generated or updated fields
     * @throws IllegalArgumentException if policy is null or invalid
     */
    AutonomyPolicyRule save(AutonomyPolicyRule policy);

    void softDelete(AutonomyPolicyRuleId id, Instant now);
}
