package com.tchalanet.server.core.autonomy.infra.persistence.mapper;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.infra.persistence.entity.AutonomyPolicyRuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AutonomyPolicyRuleMapper {

    public AutonomyPolicyRule toDomain(AutonomyPolicyRuleJpaEntity entity) {
        return new AutonomyPolicyRule(
                entity.getId(),
                entity.getTenantId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getLevel(),
                entity.isRequireApprovalOnBlock(),
                entity.getApprovalRole(),
                entity.isEnabled(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getVersion()
        );
    }

    public AutonomyPolicyRuleJpaEntity toEntity(AutonomyPolicyRule domain) {
        AutonomyPolicyRuleJpaEntity entity = new AutonomyPolicyRuleJpaEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setTargetType(domain.targetType());
        entity.setTargetId(domain.targetId());
        entity.setLevel(domain.level());
        entity.setRequireApprovalOnBlock(domain.requireApprovalOnBlock());
        entity.setApprovalRole(domain.approvalRole());
        entity.setEnabled(domain.enabled());
        entity.setStartsAt(domain.startsAt());
        entity.setEndsAt(domain.endsAt());
        entity.setVersion(domain.version());
        return entity;
    }
}
