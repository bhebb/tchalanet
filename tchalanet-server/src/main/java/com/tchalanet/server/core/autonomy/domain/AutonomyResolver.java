package com.tchalanet.server.core.autonomy.domain;

import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.ApprovalRole;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyLevel;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRuleRule;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Service for resolving the applicable autonomy policy for a given transaction context.
 *
 * The resolver follows a hierarchy to find the most specific autonomy policy:
 * 1. Agent-specific policy
 * 2. Terminal-specific policy
 * 3. Outlet-specific policy
 * 4. Tenant-wide policy
 * 5. Default policy (PARTIAL autonomy requiring OPERATOR approval)
 */
@Service
public final class AutonomyResolver {

  private final AutonomyPolicyRuleRuleRepositoryPort repo; // port

  public AutonomyResolver(AutonomyPolicyRuleRuleRepositoryPort repo) {
    this.repo = repo;
  }

  public ResolvedAutonomy resolve(UUID tenantId, UUID agentId, UUID terminalId, UUID outletId, Instant now) {
    var p = repo.findActive(tenantId, AutonomyTargetType.AGENT, agentId, now)
        .or(() -> repo.findActive(tenantId, AutonomyTargetType.TERMINAL, terminalId, now))
        .or(() -> repo.findActive(tenantId, AutonomyTargetType.OUTLET, outletId, now))
        .or(() -> repo.findActive(tenantId, AutonomyTargetType.TENANT, tenantId, now))
        .orElseGet(this::defaultPolicy);

    return new ResolvedAutonomy(
        p.level(),
        p.requireApprovalOnBlock(),
        p.approvalRole() == null ? ApprovalRole.OPERATOR : p.approvalRole()
    );
  }

  private AutonomyPolicyRuleRule defaultPolicy() {
    return new AutonomyPolicyRuleRule(
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
