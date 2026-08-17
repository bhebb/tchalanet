package com.tchalanet.server.features.tenantadmin.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.query.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.api.query.ListTenantLimitAssignmentsView;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantAdminPoliciesOverviewServiceTest {

  @Test
  void getOverviewWithActiveAssignmentsBuildsGlobalRulesAndSummary() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var assignmentId = LimitAssignmentId.of(UUID.randomUUID());

    var ruleSpec =
        new LimitRuleSpec(
            RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
            "Exposition max par sélection",
            "Plafond de mise totale par sélection et par tirage.",
            BreachOutcome.BLOCK,
            "exposure",
            false,
            null);

    var assignment =
        new ListLimitAssignmentsView.Item(
            assignmentId,
            RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
            true,
            BreachOutcome.BLOCK,
            null,
            null,
            null);

    var assignmentsView =
        new ListLimitAssignmentsView(LimitScopeQueryRef.tenant(tenantId), List.of(assignment));
    var tenantAssignmentsView =
        new ListTenantLimitAssignmentsView(
            List.of(
                new ListTenantLimitAssignmentsView.Item(
                    assignmentId,
                    RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
                    LimitScopeQueryRef.tenant(tenantId),
                    true,
                    BreachOutcome.BLOCK,
                    null,
                    null,
                    null)));

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any()))
        .thenReturn(List.of(ruleSpec))
        .thenReturn(assignmentsView)
        .thenReturn(tenantAssignmentsView)
        .thenReturn(emptyPage());
    var drawChannelCatalog = mock(DrawChannelCatalog.class);
    when(drawChannelCatalog.listAllFull(tenantId)).thenReturn(List.of());

    var service = new TenantAdminPoliciesOverviewService(queryBus, drawChannelCatalog);
    var result = service.getOverview(ctx(tenantId));

    assertThat(result.globalRules()).hasSize(1);
    assertThat(result.globalRules().getFirst().spec().ruleKey())
        .isEqualTo(RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW);
    assertThat(result.globalRules().getFirst().assignment().enabled()).isTrue();

    assertThat(result.summary().activeRules()).isEqualTo(1);
    assertThat(result.summary().globalRules()).isEqualTo(1);
    assertThat(result.summary().warnings()).isEqualTo(0);

    // no warnings → no alerts
    assertThat(result.alerts()).isEmpty();

    // scope cards always present
    assertThat(result.scopeCards()).hasSize(4);
    assertThat(result.actionLinks()).hasSize(3);
  }

  @Test
  void getOverviewWithDisabledAssignmentAddsWarning() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var assignmentId = LimitAssignmentId.of(UUID.randomUUID());

    var ruleSpec =
        new LimitRuleSpec(
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            "Bloke nimewo",
            "Bloke yon nimewo sou yon tiraj.",
            BreachOutcome.BLOCK,
            "blocking",
            true,
            null);

    var disabledAssignment =
        new ListLimitAssignmentsView.Item(
            assignmentId,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            false,
            BreachOutcome.BLOCK,
            null,
            null,
            null);

    var assignmentsView =
        new ListLimitAssignmentsView(
            LimitScopeQueryRef.tenant(tenantId), List.of(disabledAssignment));
    var tenantAssignmentsView =
        new ListTenantLimitAssignmentsView(
            List.of(
                new ListTenantLimitAssignmentsView.Item(
                    assignmentId,
                    RuleKey.BLOCK_SELECTION_PER_DRAW,
                    LimitScopeQueryRef.tenant(tenantId),
                    false,
                    BreachOutcome.BLOCK,
                    null,
                    null,
                    null)));

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any()))
        .thenReturn(List.of(ruleSpec))
        .thenReturn(assignmentsView)
        .thenReturn(tenantAssignmentsView)
        .thenReturn(emptyPage());
    var drawChannelCatalog = mock(DrawChannelCatalog.class);
    when(drawChannelCatalog.listAllFull(tenantId)).thenReturn(List.of());

    var service = new TenantAdminPoliciesOverviewService(queryBus, drawChannelCatalog);
    var result = service.getOverview(ctx(tenantId));

    assertThat(result.summary().activeRules()).isEqualTo(0);
    assertThat(result.summary().warnings()).isEqualTo(1);
    assertThat(result.alerts()).hasSize(1);
    assertThat(result.alerts().getFirst().severity()).isEqualTo("warning");
  }

  @Test
  void getOverviewWithNullAssignmentsReturnsEmptyGlobalRules() {
    var tenantId = TenantId.of(UUID.randomUUID());

    var ruleSpec =
        new LimitRuleSpec(
            RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
            "Plafon pa nimewo",
            "Limite kantite total ki ka vann sou yon nimewo pou yon tiraj.",
            BreachOutcome.BLOCK,
            "exposure",
            false,
            null);

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any()))
        .thenReturn(List.of(ruleSpec))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(emptyPage());
    var drawChannelCatalog = mock(DrawChannelCatalog.class);
    when(drawChannelCatalog.listAllFull(tenantId)).thenReturn(List.of());

    var service = new TenantAdminPoliciesOverviewService(queryBus, drawChannelCatalog);
    var result = service.getOverview(ctx(tenantId));

    // rule has no assignment → filtered out by the null assignment check
    assertThat(result.globalRules()).isEmpty();
    assertThat(result.summary().activeRules()).isEqualTo(0);
    assertThat(result.alerts()).isEmpty();
  }

  private static TchRequestContext ctx(TenantId tenantId) {
    return new TchRequestContext(
        "tenant",
        tenantId.value(),
        "tenant",
        tenantId.value(),
        null,
        Set.of(),
        Set.of(),
        null,
        "request-id",
        null,
        null,
        false,
        null,
        "active",
        null,
        null,
        tenantId,
        ZoneId.of("America/Port-au-Prince"),
        null,
        null,
        null,
        null,
        Set.of(),
        Set.of(),
        null);
  }

  private static TchPage<Object> emptyPage() {
    return new TchPage<>(List.of(), 0, 500, 0L, 0, true, false, false);
  }
}
