package com.tchalanet.server.core.autonomy.infra.persistence.mapper;

import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyPolicyRuleId;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import com.tchalanet.server.core.autonomy.infra.persistence.entity.AutonomyPolicyRuleJpaEntity;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import org.springframework.stereotype.Component;

@Component
public class AutonomyPolicyRuleMapper {

  public AutonomyPolicyRule toDomain(AutonomyPolicyRuleJpaEntity e) {
    if (e == null) return null;
    AutonomyPolicyRule d = new AutonomyPolicyRule();

    d.setId(AutonomyPolicyRuleId.of(e.getId()));
    d.setTargetType(e.getTargetType());
    d.setTargetId(AutonomyTargetId.of(e.getTargetId()));

    d.setLevel(e.getLevel());
    d.setRequireApprovalOnBlock(e.isRequireApprovalOnBlock());
    d.setApprovalRole(e.getApprovalRole());
    d.setEnabled(e.isEnabled());

    d.setStartsAt(e.getStartsAt() == null ? null : e.getStartsAt().atOffset(java.time.ZoneOffset.UTC));
    d.setEndsAt(e.getEndsAt() == null ? null : e.getEndsAt().atOffset(java.time.ZoneOffset.UTC));

    d.setVersion(e.getVersion());
    d.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC));
    d.setUpdatedAt(e.getUpdatedAt() == null ? null : e.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC));

    d.setDeleted(e.getDeletedAt() != null);

    // NOTE: tenant_id and deleted_at are infra concerns and intentionally NOT mapped on domain
    return d;
  }

  public AutonomyPolicyRuleJpaEntity toEntity(AutonomyPolicyRule d) {
    if (d == null) return null;
    AutonomyPolicyRuleJpaEntity e = new AutonomyPolicyRuleJpaEntity();

    // If domain has id, map it; otherwise JPA may generate on persist
    if (d.getId() != null) e.setId(d.getId().value());

    e.setTargetType(d.getTargetType());
    e.setTargetId(d.getTargetId().value());

    e.setLevel(d.getLevel());
    e.setRequireApprovalOnBlock(d.isRequireApprovalOnBlock());
    e.setApprovalRole(d.getApprovalRole());
    e.setEnabled(d.isEnabled());

    e.setStartsAt(d.getStartsAt() == null ? null : d.getStartsAt().toInstant());
    e.setEndsAt(d.getEndsAt() == null ? null : d.getEndsAt().toInstant());

    // version/createdAt/updatedAt handled by entity lifecycle or adapter
    e.setVersion(d.getVersion() == null ? 0L : d.getVersion());
    // createdAt/updatedAt are Instant on entity; adapter or JPA lifecycle should manage them
    return e;
  }
}
