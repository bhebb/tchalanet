package com.tchalanet.server.core.autonomy.infra.web.admin.model;

import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;

public record AutonomyMetaResponse(
    boolean configured,
    boolean deleted,
    AutonomyPolicyRuleId policyRuleId
) {
}
