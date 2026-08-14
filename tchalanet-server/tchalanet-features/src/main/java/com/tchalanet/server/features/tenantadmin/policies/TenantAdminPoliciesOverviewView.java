package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.core.limitpolicy.api.query.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record TenantAdminPoliciesOverviewView(
    Summary summary,
    List<ScopeCard> scopeCards,
    List<ActionLink> actionLinks,
    List<Alert> alerts,
    List<GlobalRule> globalRules,
    List<ActiveLimit> activeLimits) {

  public record Summary(
      int activeRules,
      int globalRules,
      int sellerOverrides,
      int channelOverrides,
      int numberRules,
      int warnings) {}

  public record ScopeCard(
      String id,
      String icon,
      String title,
      String description,
      String metric,
      String status,
      String route,
      String cta) {}

  public record ActionLink(
      String id, String icon, String label, String description, String route) {}

  public record Alert(String severity, String message) {}

  public record GlobalRule(LimitRuleSpec spec, ListLimitAssignmentsView.Item assignment) {}

  public record ActiveLimit(
      UUID assignmentId,
      String ruleKey,
      String group,
      String targetType,
      UUID targetId,
      String targetLabel,
      String targetCode,
      boolean enabled,
      String onBreach,
      JsonNode params,
      Instant startsAt,
      Instant endsAt,
      List<String> actions) {}
}
