package com.tchalanet.server.core.autonomy.infra.persistence;

import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetId;
import org.springframework.stereotype.Component;

@Component
public class AutonomyPolicyRuleMapper {

    public AutonomyPolicyRule toDomain(AutonomyPolicyRuleJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new AutonomyPolicyRule(
            AutonomyPolicyRuleId.of(entity.getId()),
            entity.getTargetType(),
            AutonomyTargetId.of(entity.getTargetId()),
            entity.getLevel(),
            entity.isRequireApprovalOnBlock(),
            entity.getApprovalRole(),
            entity.isEnabled(),
            entity.getStartsAt(),
            entity.getEndsAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt() != null);
    }

    public AutonomyPolicyRuleJpaEntity toEntity(AutonomyPolicyRule rule) {
        if (rule == null) {
            return null;
        }

        var entity = new AutonomyPolicyRuleJpaEntity();

        if (rule.id() != null) {
            entity.setId(rule.id().value());
        }

        entity.setTargetType(rule.targetType());
        entity.setTargetId(rule.targetId().value());
        entity.setLevel(rule.level());
        entity.setRequireApprovalOnBlock(rule.requireApprovalOnBlock());
        entity.setApprovalRole(rule.approvalRole());
        entity.setEnabled(rule.enabled());
        entity.setStartsAt(rule.startsAt());
        entity.setEndsAt(rule.endsAt());

        return entity;
    }
}
