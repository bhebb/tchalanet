package com.tchalanet.server.core.limitpolicy.domain.resolver;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Minimal resolver stub used until the real domain resolver is implemented.
 */
@Component
public class LimitResolver {

  public EffectiveLimits resolve(
      List<LimitDefinition> defs,
      List<LimitAssignment> assigns,
      LimitContext ctx
  ) {
    // 1) map defaults
    Map<RuleKey, BigDecimal> resolved = new HashMap<>();

    Map<com.tchalanet.server.common.types.id.LimitDefinitionId, LimitDefinition> defById = new HashMap<>();
    for (var d : defs) {
      defById.put(d.id(), d);
      BigDecimal dv = extractValueFromParams(d.params());
      if (dv != null) {
        resolved.put(d.ruleKey(), dv);
      }
    }

    // 2) apply "best assignment" per rule
    var best = pickBestAssignments(assigns, ctx, defById);

    for (var e : best.entrySet()) {
      var ruleKey = e.getKey();
      var assignment = e.getValue();
      LimitDefinition def = defById.get(assignment.limitDefinitionId());
      BigDecimal val = getAssignmentValue(assignment, def);
      if (val != null) resolved.put(ruleKey, val);
    }

    return new EffectiveLimits(Collections.unmodifiableMap(resolved));
  }

  private Map<RuleKey, LimitAssignment> pickBestAssignments(
      List<LimitAssignment> assigns,
      LimitContext ctx,
      Map<com.tchalanet.server.common.types.id.LimitDefinitionId, LimitDefinition> defById) {

    Map<RuleKey, LimitAssignment> best = new HashMap<>();

    for (var a : assigns) {
      if (!a.isActiveAt(ctx.now())) continue;

      LimitDefinition def = defById.get(a.limitDefinitionId());
      if (def == null) continue;

      int score = score(a.target(), ctx);
      if (score < 0) continue;

      var ruleKey = def.ruleKey();
      var existing = best.get(ruleKey);
      if (existing == null || score > score(existing.target(), ctx)) {
        best.put(ruleKey, a);
      }
    }
    return best;
  }

  private BigDecimal getAssignmentValue(LimitAssignment a, LimitDefinition def) {
    BigDecimal v = extractValueFromParams(a.paramsOverride());
    if (v != null) return v;
    return extractValueFromParams(def.params());
  }

  private BigDecimal extractValueFromParams(JsonNode params) {
    if (params == null) return null;
    // expect structure like { "value": 123.45 }
    if (params.has("value")) {
      JsonNode n = params.get("value");
      if (n.isNumber()) return n.decimalValue();
      try {
        return new BigDecimal(n.asText());
      } catch (Exception ex) {
        return null;
      }
    }
    return null;
  }

  /**
   * Specificity hierarchy (higher = wins over lower):
   * Tenant(10) < DrawChannel(40) < Outlet(50) < Agent(60) < Terminal(70).
   *
   * <p>A terminal is the most specific POS context: it identifies the exact device. A target that
   * does not match the current context returns -1 and is excluded.
   */
  private int score(LimitTarget target, LimitContext ctx) {
    return switch (target) {
      case LimitTarget.TenantTarget ignored -> 10; // INTENTIONAL: tenant fallback.
      case LimitTarget.OutletTarget t -> (ctx.outletId() != null && ctx.outletId().equals(t.id())) ? 50 : -1; // INTENTIONAL: outlet beats draw channel.
      case LimitTarget.AgentTarget t -> (ctx.agentId() != null && ctx.agentId().equals(t.id())) ? 60 : -1; // INTENTIONAL: agent beats outlet.
      case LimitTarget.TerminalTarget t -> (ctx.terminalId() != null && ctx.terminalId().equals(t.id())) ? 70 : -1; // INTENTIONAL: terminal beats agent.
      case LimitTarget.DrawChannelTarget t -> (ctx.drawChannelId() != null && ctx.drawChannelId().equals(t.id())) ? 40 : -1; // INTENTIONAL: draw channel beats tenant.
    };
  }
}
