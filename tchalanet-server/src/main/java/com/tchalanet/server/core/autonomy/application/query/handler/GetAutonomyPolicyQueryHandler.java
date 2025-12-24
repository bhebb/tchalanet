package com.tchalanet.server.core.autonomy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleQuery;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class GetAutonomyPolicyRuleQueryHandler implements QueryHandler<GetAutonomyPolicyRuleQuery, GetAutonomyPolicyRuleResult> {

    private final AutonomyPolicyRuleRuleRepositoryPort repository;

    @Override
    public GetAutonomyPolicyRuleResult handle(GetAutonomyPolicyRuleQuery query) {
        var policy = repository.findByTarget(query.tenantId(), query.targetType(), query.targetId())
                .orElseThrow(() -> new RuntimeException("Policy not found")); // or handle differently

        return new GetAutonomyPolicyRuleResult(
                policy.id(),
                policy.tenantId(),
                policy.targetType(),
                policy.targetId(),
                policy.level(),
                policy.requireApprovalOnBlock(),
                policy.approvalRole(),
                policy.enabled(),
                policy.startsAt(),
                policy.endsAt()
        );
    }
}
