package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.query.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListAvailableLimitRulesQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAdminPoliciesOverviewService {

  private final QueryBus queryBus;

  public TenantAdminPoliciesOverviewView getOverview(TchRequestContext ctx) {
    var tenantId = ctx.effectiveTenantIdRequired();
    List<LimitRuleSpec> rules = queryBus.ask(new ListAvailableLimitRulesQuery());
    ListLimitAssignmentsView assignments = queryBus.ask(
        new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));

    List<ListLimitAssignmentsView.Item> tenantAssignments =
        assignments != null && assignments.items() != null ? assignments.items() : List.of();

    Map<RuleKey, ListLimitAssignmentsView.Item> assignmentsByRule =
        tenantAssignments.stream()
            .collect(Collectors.toMap(
                ListLimitAssignmentsView.Item::ruleKey,
                Function.identity(),
                (left, right) -> left));

    List<TenantAdminPoliciesOverviewView.GlobalRule> globalRules = rules.stream()
        .map(spec -> new TenantAdminPoliciesOverviewView.GlobalRule(
            spec,
            assignmentsByRule.get(spec.ruleKey())))
        .filter(row -> row.assignment() != null)
        .toList();

    int activeGlobalRules = (int) globalRules.stream()
        .filter(row -> row.assignment().enabled())
        .count();
    int warnings = (int) globalRules.stream()
        .filter(row -> !row.assignment().enabled())
        .count();
    int numberRules = (int) globalRules.stream()
        .filter(row -> row.assignment().enabled())
        .filter(row -> row.spec().ruleKey().name().contains("SELECTION"))
        .count();

    var summary = new TenantAdminPoliciesOverviewView.Summary(
        activeGlobalRules,
        globalRules.size(),
        0,
        0,
        numberRules,
        warnings);

    return new TenantAdminPoliciesOverviewView(
        summary,
        scopeCards(summary),
        actionLinks(),
        alerts(summary),
        globalRules);
  }

  private List<TenantAdminPoliciesOverviewView.ScopeCard> scopeCards(
      TenantAdminPoliciesOverviewView.Summary summary) {
    return List.of(
        new TenantAdminPoliciesOverviewView.ScopeCard(
            "global",
            "tune",
            "Globales",
            "Règles appliquées par défaut à toutes les ventes.",
            summary.activeRules() + " active(s)",
            summary.activeRules() > 0 ? "OK" : "À configurer",
            "global",
            "Voir"),
        new TenantAdminPoliciesOverviewView.ScopeCard(
            "sellers",
            "storefront",
            "Vendeurs",
            "Exceptions appliquées à un terminal ou vendeur précis.",
            "Par vendeur",
            "À surveiller",
            "seller-terminal",
            "Voir"),
        new TenantAdminPoliciesOverviewView.ScopeCard(
            "numbers",
            "pin",
            "Numéros",
            "Suivi des plafonds sur les sélections à risque.",
            summary.numberRules() + " règle(s)",
            summary.numberRules() > 0 ? "OK" : "À configurer",
            "number",
            "Voir exposition"),
        new TenantAdminPoliciesOverviewView.ScopeCard(
            "channels",
            "event",
            "Canaux",
            "Limites spécifiques à un canal de tirage.",
            "Par canal",
            "À surveiller",
            "draw",
            "Voir"));
  }

  private List<TenantAdminPoliciesOverviewView.ActionLink> actionLinks() {
    return List.of(
        new TenantAdminPoliciesOverviewView.ActionLink(
            "block-number",
            "block",
            "Limiter un numéro",
            "Définir un plafond de mise ou de gain potentiel sur une sélection.",
            "number"),
        new TenantAdminPoliciesOverviewView.ActionLink(
            "draw-limit",
            "event",
            "Limiter un canal",
            "Définir une limite spécifique pour un canal de tirage.",
            "draw"),
        new TenantAdminPoliciesOverviewView.ActionLink(
            "seller-limit",
            "storefront",
            "Limiter un vendeur",
            "Créer une limite pour un terminal ou vendeur précis.",
            "seller-terminal"));
  }

  private List<TenantAdminPoliciesOverviewView.Alert> alerts(
      TenantAdminPoliciesOverviewView.Summary summary) {
    if (summary.warnings() == 0) {
      return List.of();
    }
    return List.of(new TenantAdminPoliciesOverviewView.Alert(
        "warning",
        summary.warnings() + " règle(s) globale(s) sont désactivées."));
  }
}
