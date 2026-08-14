package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.query.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListAvailableLimitRulesQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.api.query.ListTenantLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListTenantLimitAssignmentsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TenantAdminPoliciesOverviewService {

  private final QueryBus queryBus;
  private final DrawChannelCatalog drawChannelCatalog;

  public TenantAdminPoliciesOverviewView getOverview(TchRequestContext ctx) {
    var tenantId = ctx.tenantIdRequired();
    List<LimitRuleSpec> rules = queryBus.ask(new ListAvailableLimitRulesQuery());
    ListLimitAssignmentsView assignments =
        queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));
    ListTenantLimitAssignmentsView tenantWideAssignments =
        queryBus.ask(new ListTenantLimitAssignmentsQuery(tenantId));

    List<ListLimitAssignmentsView.Item> tenantAssignments =
        assignments != null && assignments.items() != null ? assignments.items() : List.of();
    List<ListTenantLimitAssignmentsView.Item> allAssignments =
        tenantWideAssignments != null && tenantWideAssignments.items() != null
            ? tenantWideAssignments.items()
            : List.of();

    Map<RuleKey, ListLimitAssignmentsView.Item> assignmentsByRule =
        tenantAssignments.stream()
            .collect(
                Collectors.toMap(
                    ListLimitAssignmentsView.Item::ruleKey,
                    Function.identity(),
                    (left, right) -> left));

    List<TenantAdminPoliciesOverviewView.GlobalRule> globalRules =
        rules.stream()
            .map(
                spec ->
                    new TenantAdminPoliciesOverviewView.GlobalRule(
                        spec, assignmentsByRule.get(spec.ruleKey())))
            .filter(row -> row.assignment() != null)
            .toList();

    int activeRules =
        (int) allAssignments.stream().filter(ListTenantLimitAssignmentsView.Item::enabled).count();
    int activeGlobalRules =
        (int) tenantAssignments.stream().filter(ListLimitAssignmentsView.Item::enabled).count();
    int warnings = (int) allAssignments.stream().filter(row -> !row.enabled()).count();
    int numberRules =
        (int)
            allAssignments.stream()
                .filter(ListTenantLimitAssignmentsView.Item::enabled)
                .filter(row -> row.ruleKey().name().contains("SELECTION"))
                .count();
    int sellerOverrides =
        (int)
            allAssignments.stream()
                .filter(
                    row -> row.limitScopeRef().type() == LimitScopeQueryRef.Type.SELLER_TERMINAL)
                .count();
    int channelOverrides =
        (int)
            allAssignments.stream()
                .filter(row -> row.limitScopeRef().type() == LimitScopeQueryRef.Type.DRAW_CHANNEL)
                .count();

    var summary =
        new TenantAdminPoliciesOverviewView.Summary(
            activeRules,
            activeGlobalRules,
            sellerOverrides,
            channelOverrides,
            numberRules,
            warnings);

    return new TenantAdminPoliciesOverviewView(
        summary,
        scopeCards(summary),
        actionLinks(),
        alerts(summary),
        globalRules,
        activeLimits(tenantId, allAssignments));
  }

  private List<TenantAdminPoliciesOverviewView.ActiveLimit> activeLimits(
      com.tchalanet.server.common.types.id.TenantId tenantId,
      List<ListTenantLimitAssignmentsView.Item> assignments) {
    var drawChannelLabels =
        drawChannelCatalog.listAllFull(tenantId).stream()
            .collect(
                Collectors.toMap(
                    row -> row.id().value(), Function.identity(), (left, right) -> left));
    var terminalLabels =
        queryBus
            .ask(
                new ListSellerTerminalsQuery(
                    tenantId,
                    SellerTerminalSearchCriteria.empty(),
                    new TchPageRequest(PageRequest.of(0, 500))))
            .items()
            .stream()
            .collect(
                Collectors.toMap(
                    row -> row.id().value(), Function.identity(), (left, right) -> left));

    return assignments.stream()
        .map(item -> toActiveLimit(item, drawChannelLabels, terminalLabels))
        .toList();
  }

  private TenantAdminPoliciesOverviewView.ActiveLimit toActiveLimit(
      ListTenantLimitAssignmentsView.Item item,
      Map<UUID, DrawChannelView> drawChannelLabels,
      Map<UUID, SellerTerminalSummaryRow> terminalLabels) {
    var scope = item.limitScopeRef();
    return new TenantAdminPoliciesOverviewView.ActiveLimit(
        item.id().value(),
        item.ruleKey().name(),
        groupFor(item.ruleKey()),
        scope.type().name(),
        scope.id(),
        targetLabel(scope, drawChannelLabels, terminalLabels),
        targetCode(scope, drawChannelLabels, terminalLabels),
        item.enabled(),
        item.onBreach().name(),
        item.params(),
        item.startsAt(),
        item.endsAt(),
        actionsFor(item));
  }

  private static String groupFor(RuleKey ruleKey) {
    return switch (ruleKey) {
      case BLOCK_SELECTION_PER_DRAW -> "NUMBER_BLOCK";
      case MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW, MAX_SALES_COUNT_PER_SELECTION_PER_DRAW ->
          "NUMBER_CAP";
      case MAX_LINES_PER_TICKET,
          MAX_STAKE_PER_LINE,
          MAX_STAKE_PER_TICKET,
          MAX_STAKE_PER_SELECTION_PER_TICKET,
          MAX_STAKE_PER_BET_TYPE_PER_TICKET,
          MAX_SALES_COUNT_PER_TICKET ->
          "TICKET_LIMIT";
      case MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW,
          MAX_STAKE_PER_AGENT_PER_DRAW,
          MAX_STAKE_PER_OUTLET_PER_DRAW ->
          "SELLER_LIMIT";
      case BLOCK_BET_TYPE -> "ADVANCED";
    };
  }

  private static List<String> actionsFor(ListTenantLimitAssignmentsView.Item item) {
    return item.enabled()
        ? List.of("EDIT", "DISABLE", "DELETE")
        : List.of("EDIT", "ENABLE", "DELETE");
  }

  private static String targetLabel(
      LimitScopeQueryRef scope,
      Map<UUID, DrawChannelView> drawChannels,
      Map<UUID, SellerTerminalSummaryRow> terminals) {
    return switch (scope.type()) {
      case TENANT -> "TENANT";
      case DRAW_CHANNEL -> {
        var channel = drawChannels.get(scope.id());
        yield channel != null
            ? firstPresent(channel.label(), channel.name(), channel.code())
            : null;
      }
      case SELLER_TERMINAL -> {
        var terminal = terminals.get(scope.id());
        yield terminal != null
            ? firstPresent(terminal.displayName(), terminal.terminalCode(), terminal.email())
            : null;
      }
      case AGENT -> null;
    };
  }

  private static String targetCode(
      LimitScopeQueryRef scope,
      Map<UUID, DrawChannelView> drawChannels,
      Map<UUID, SellerTerminalSummaryRow> terminals) {
    return switch (scope.type()) {
      case TENANT -> "TENANT";
      case DRAW_CHANNEL -> {
        var channel = drawChannels.get(scope.id());
        yield channel != null ? channel.code() : null;
      }
      case SELLER_TERMINAL -> {
        var terminal = terminals.get(scope.id());
        yield terminal != null ? terminal.terminalCode() : null;
      }
      case AGENT -> null;
    };
  }

  private static String firstPresent(String... values) {
    for (var value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
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
            "Définir un plafond de mise, de ventes ou bloquer une sélection.",
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
    return List.of(
        new TenantAdminPoliciesOverviewView.Alert(
            "warning", summary.warnings() + " règle(s) globale(s) sont désactivées."));
  }
}
