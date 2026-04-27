package com.tchalanet.server.core.autonomy.application.service;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.application.service.model.AutonomyResolveRequest;
import com.tchalanet.server.core.autonomy.application.service.model.ResolvedAutonomy;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolveAutonomyPolicyService {

  private final AutonomyPolicyRuleRepositoryPort repo;
  private final TchContextResolver tchContextResolver;

  public ResolvedAutonomy resolve(AutonomyResolveRequest req) {
    Instant now = req.now() == null ? Instant.now() : req.now();

    UUID agentUuid = req.agentId() == null ? null : req.agentId().value();
    UUID terminalUuid = req.terminalId() == null ? null : req.terminalId().value();
    UUID outletUuid = req.outletId() == null ? null : req.outletId().value();

    // Resolution order: AGENT -> TERMINAL -> OUTLET -> TENANT -> DEFAULT
    AutonomyPolicyRule p = null;
    AutonomyTargetType sourceType = null;
    UUID sourceId = null;
    boolean isDefault = false;

    if (agentUuid != null) {
      p = repo.findActiveRuntime(AutonomyTargetType.AGENT, agentUuid, now).orElse(null);
      if (p != null) {
        sourceType = AutonomyTargetType.AGENT;
        sourceId = agentUuid;
      }
    }

    if (p == null && terminalUuid != null) {
      p = repo.findActiveRuntime(AutonomyTargetType.TERMINAL, terminalUuid, now).orElse(null);
      if (p != null) {
        sourceType = AutonomyTargetType.TERMINAL;
        sourceId = terminalUuid;
      }
    }

    if (p == null && outletUuid != null) {
      p = repo.findActiveRuntime(AutonomyTargetType.OUTLET, outletUuid, now).orElse(null);
      if (p != null) {
        sourceType = AutonomyTargetType.OUTLET;
        sourceId = outletUuid;
      }
    }

    if (p == null) {
      // Tenant-level resolution: tenant is implicit via context (RLS)
      UUID tenantUuid = tchContextResolver.currentOrThrow().tenantUuid();
      p = repo.findActiveRuntime(AutonomyTargetType.TENANT, tenantUuid, now).orElse(null);
      if (p != null) {
        sourceType = AutonomyTargetType.TENANT;
        sourceId = tenantUuid;
      }
    }

    if (p == null) {
      // default policy
      p = defaultPolicy();
      sourceType = null;
      sourceId = null;
      isDefault = true;
    }

    var level = p.getLevel();
    var requireApproval = p.isRequireApprovalOnBlock();
    var approval = p.getApprovalRole() == null ? com.tchalanet.server.common.types.enums.ApprovalRole.OPERATOR : p.getApprovalRole();

    ResolvedAutonomy.Source source = new ResolvedAutonomy.Source(
        sourceType,
        sourceId == null ? null : com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId.of(sourceId),
        p.getId() == null ? null : p.getId().value(),
        p.getVersion() == null ? 0L : p.getVersion(),
        isDefault);

    return new ResolvedAutonomy(level, requireApproval, approval, source);
  }

  private AutonomyPolicyRule defaultPolicy() {
    AutonomyPolicyRule d = new AutonomyPolicyRule();
    d.setLevel(com.tchalanet.server.common.types.enums.AutonomyLevel.PARTIAL);
    d.setRequireApprovalOnBlock(true);
    d.setApprovalRole(com.tchalanet.server.common.types.enums.ApprovalRole.OPERATOR);
    d.setEnabled(true);
    d.setVersion(0L);
    return d;
  }
}
