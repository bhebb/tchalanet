package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.autonomy.application.command.handler.UpsertAutonomyPolicyRuleCommandHandler;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleCommand;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionResult;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitDefinitionResult;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsByTargetQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsView;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAdminPoliciesOrchestrator {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final UpsertAutonomyPolicyRuleCommandHandler autonomyHandler;

  // Limit definitions
  public ListLimitDefinitionsView listLimitDefinitions() {
    return queryBus.send(new ListLimitDefinitionsQuery());
  }

  public UpsertLimitDefinitionResult upsertLimitDefinition(UpsertLimitDefinitionCmd dto) {
    var cmd = new UpsertLimitDefinitionCommand(
        dto.ruleKey(), dto.enabled(), dto.onBreach(), dto.params(), dto.appliesTo()
    );
    return commandBus.send(cmd);
  }

  public DeleteLimitDefinitionResult deleteLimitDefinition(LimitDefinitionId id) {
    var cmd = new DeleteLimitDefinitionCommand(id);
    return commandBus.send(cmd);
  }

  // Limit assignments
  public ListLimitAssignmentsView listAssignmentsByTarget(LimitTarget target) {
    return queryBus.send(new ListLimitAssignmentsByTargetQuery(target));
  }

  public UpsertLimitAssignmentResult upsertLimitAssignment(UpsertLimitAssignmentCmd dto) {
    var cmd = new UpsertLimitAssignmentCommand(
        dto.limitDefinitionId(), dto.target(), dto.enabled(), dto.startsAt(), dto.endsAt(), dto.paramsOverride(), dto.appliesToOverride()
    );
    return commandBus.send(cmd);
  }

  public DeleteLimitAssignmentResult deleteLimitAssignment(com.tchalanet.server.common.types.id.LimitAssignmentId id) {
    var cmd = new DeleteLimitAssignmentCommand(id);
    return commandBus.send(cmd);
  }

  // Autonomy
  public AutonomyOverviewView getAutonomyOverview(AutonomyTargetType targetType, UUID targetId) {
    var q = new GetAutonomyOverviewQuery(targetType, targetId);
    return queryBus.send(q);
  }

  public AutonomyOverviewView upsertAutonomyRule(TchRequestContext ctx, UpsertAutonomyPolicyRuleCommand cmd) {
    // handler will apply tenant context via TchContextResolver if needed
    autonomyHandler.handle(cmd);

    // derive effective target uuid for overview query
    UUID effectiveTargetUuid = cmd.getTargetId() == null ? null : cmd.getTargetId().value();
    if (cmd.getTargetType() == AutonomyTargetType.TENANT && effectiveTargetUuid == null) {
      effectiveTargetUuid = ctx.tenantIdSafe().value();
    }

    return getAutonomyOverview(cmd.getTargetType(), effectiveTargetUuid);
  }

  // New: accept feature DTO and map to core command
  public AutonomyOverviewView upsertAutonomyRule(TchRequestContext ctx, com.tchalanet.server.features.tenantadmin.policies.model.UpsertAutonomyRuleRequest req) {
    var targetId = req.targetId() == null ? null : AutonomyTargetId.of(req.targetId());
    var cmd = new UpsertAutonomyPolicyRuleCommand(
        req.targetType(),
        targetId,
        req.level(),
        req.requireApprovalOnBlock(),
        req.approvalRole(),
        req.enabled(),
        req.startsAt(),
        req.endsAt(),
        req.expectedVersion()
    );
    return upsertAutonomyRule(ctx, cmd);
  }

  // Policies overview aggregator
  @SuppressWarnings("unused")
  public com.tchalanet.server.features.tenantadmin.policies.model.PoliciesOverviewView getPoliciesOverview(TchRequestContext ctx) {
    var defs = listLimitDefinitions();
    int defsCount = defs.items() == null ? 0 : defs.items().size();

    var tenantAssignments = listAssignmentsByTarget(LimitTarget.tenant());
    int tenantAsgCount = tenantAssignments.items() == null ? 0 : tenantAssignments.items().size();

    var autonomy = getAutonomyOverview(AutonomyTargetType.TENANT, ctx.tenantIdSafe().value());
    boolean autonomyConfigured = autonomy.rule() != null;
    String autonomyLevel = autonomy.rule() == null ? null : autonomy.rule().level().name();

    return new com.tchalanet.server.features.tenantadmin.policies.model.PoliciesOverviewView(defsCount, tenantAsgCount, autonomyConfigured, autonomyLevel);
  }

  // DTO shapes used internally for mapping
  public record UpsertLimitDefinitionCmd(
      RuleKey ruleKey, boolean enabled, BreachOutcome onBreach, JsonNode params, JsonNode appliesTo) {}

  public record UpsertLimitAssignmentCmd(
      LimitDefinitionId limitDefinitionId,
      LimitTarget target,
      boolean enabled,
      Instant startsAt,
      Instant endsAt,
      JsonNode paramsOverride,
      JsonNode appliesToOverride) {}
}
