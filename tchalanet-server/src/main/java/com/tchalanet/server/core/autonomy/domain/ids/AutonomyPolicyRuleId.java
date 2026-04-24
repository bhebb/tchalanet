package com.tchalanet.server.core.autonomy.domain.ids;

import java.util.UUID;

public record AutonomyPolicyRuleId(UUID value) {
    public static AutonomyPolicyRuleId of(UUID id) { return new AutonomyPolicyRuleId(id); }
}
