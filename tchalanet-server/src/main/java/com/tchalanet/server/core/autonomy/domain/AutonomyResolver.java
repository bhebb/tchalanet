package com.tchalanet.server.core.autonomy.domain;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.OutletId;

import java.time.Instant;

import org.springframework.stereotype.Service;

/**
 * Service for resolving the applicable autonomy policy for a given transaction context.
 */
@Service
public final class AutonomyResolver {

  private final AutonomyPolicyRuleRepositoryPort repo; // port

  public AutonomyResolver(AutonomyPolicyRuleRepositoryPort repo) {
    this.repo = repo;
  }

  public ResolvedAutonomy resolve(TenantId tenantId, AgentId agentId, TerminalId terminalId, OutletId outletId, Instant now) {
    var tenantUuid = tenantId == null ? null : tenantId.uuid();
    var agentUuid = agentId == null ? null : agentId.uuid();
    var terminalUuid = terminalId == null ? null : terminalId.uuid();
    var outletUuid = outletId == null ? null : outletId.uuid();

    var p = repo.findActive(tenantId, AutonomyTargetType.AGENT, agentUuid, now)
        .or(() -> repo.findActive(tenantId, AutonomyTargetType.TERMINAL, terminalUuid, now))
        .or(() -> repo.findActive(tenantId, AutonomyTargetType.OUTLET, outletUuid, now))
        .or(() -> repo.findActive(tenantId, AutonomyTargetType.TENANT, tenantUuid, now))
        .orElseGet(this::defaultPolicy);

    return new ResolvedAutonomy(
        p.level(),
        p.requireApprovalOnBlock(),
        p.approvalRole() == null ? ApprovalRole.OPERATOR : p.approvalRole()
    );
  }

  private AutonomyPolicyRule defaultPolicy() {
    return new AutonomyPolicyRule(
        null, // id
        null, // tenantId
        null, // targetType
        null, // targetId
        AutonomyLevel.PARTIAL,
        true,
        ApprovalRole.OPERATOR,
        true, // enabled
        null, // startsAt
        null, // endsAt
        0L // version
    );
  }
}
