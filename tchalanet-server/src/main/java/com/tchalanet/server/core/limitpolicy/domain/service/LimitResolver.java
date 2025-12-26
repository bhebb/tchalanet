package com.tchalanet.server.core.limitpolicy.domain.service;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.ResolvedLimitSet;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LimitResolver {

  private final LimitDefinitionReaderPort reader;

  public ResolvedLimitSet resolve(LimitContext context) {
    List<LimitDefinition> definitions = reader.findActiveByTenantId(context.tenantId());
    List<LimitAssignment> assignments = reader.findActiveAssignmentsByTenantId(context.tenantId());

    Map<RuleKey, LimitDefinition> resolved = new HashMap<>();

    // Group assignments by ruleKey
    Map<RuleKey, List<LimitAssignment>> assignmentsByRule = assignments.stream()
        .collect(java.util.stream.Collectors.groupingBy(a -> definitions.stream()
            .filter(d -> d.id().equals(a.limitDefinitionId()))
            .findFirst().map(LimitDefinition::ruleKey).orElse(null)));

    for (LimitDefinition def : definitions) {
      if (!def.enabled()) continue;

      List<LimitAssignment> ruleAssignments = assignmentsByRule.get(def.ruleKey());
      if (ruleAssignments != null) {
        LimitAssignment highestPriority = ruleAssignments.stream()
            .filter(a -> a.enabled() && isActive(a, context.now()))
            .filter(a -> matchesTarget(a, context))
            .max(Comparator.comparingInt(a -> priority(a.targetType())))
            .orElse(null);
        if (highestPriority != null) {
          resolved.put(def.ruleKey(), def);
        }
      }
    }

    return new ResolvedLimitSet(resolved);
  }

  private boolean isActive(LimitAssignment a, Instant now) {
    return (a.startsAt() == null || !a.startsAt().isAfter(now)) &&
           (a.endsAt() == null || !a.endsAt().isBefore(now));
  }

  private boolean matchesTarget(LimitAssignment a, LimitContext context) {
    return switch (a.targetType()) {
      case TENANT -> true;
      case AGENT -> a.targetId().equals(context.agentId());
      case TERMINAL -> a.targetId().equals(context.terminalId());
      case OUTLET -> a.targetId().equals(context.outletId());
      case ZONE -> a.targetId().equals(context.zoneId());
      case RANGE -> context.rangeIds() != null && context.rangeIds().contains(a.targetId());
      case DRAWCHANNEL -> a.targetId().equals(context.drawChannelId());
      default -> false;
    };
  }

  private int priority(TargetType type) {
    return switch (type) {
      case AGENT -> 7;
      case TERMINAL -> 6;
      case OUTLET -> 5;
      case ZONE -> 4;
      case RANGE -> 3;
      case DRAWCHANNEL -> 2;
      case TENANT -> 1;
      default -> 0;
    };
  }
}
