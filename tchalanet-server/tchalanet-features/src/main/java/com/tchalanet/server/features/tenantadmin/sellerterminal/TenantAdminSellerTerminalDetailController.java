package com.tchalanet.server.features.tenantadmin.sellerterminal;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListAvailableLimitRulesQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seller-terminals")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Tenant admin • Seller terminal detail")
@RequiredArgsConstructor
public class TenantAdminSellerTerminalDetailController {

  private final QueryBus queryBus;
  private final ClientDiagnosticsApi clientDiagnosticsApi;
  private final TenantConfigApi tenantConfigApi;

  @GetMapping("/{id}/detail")
  @RequiresPermission("seller_terminal.read")
  @Operation(summary = "Get seller terminal detail aggregate")
  public ApiResponse<SellerTerminalDetailView> detail(
      @CurrentContext TchRequestContext ctx, @PathVariable SellerTerminalId id) {
    var tenantId = ctx.tenantIdRequired();
    var terminal = queryBus.ask(new GetSellerTerminalQuery(tenantId, id));
    var diagnostics = clientDiagnosticsApi.getPolicy(tenantId, id);
    var tenant = tenantConfigApi.getTenantById(new GetTenantByIdRequest(tenantId));
    return ApiResponse.success(
        new SellerTerminalDetailView(
            terminal,
            tenantSummary(tenant),
            diagnostics,
            todayStats(ctx, id),
            limits(tenantId, id)));
  }

  private TenantSummary tenantSummary(TenantConfigView tenant) {
    return new TenantSummary(tenant.tenantId(), tenant.code(), tenant.name(), tenant.displayName());
  }

  private LimitBundle limits(TenantId tenantId, SellerTerminalId id) {
    List<LimitRuleSpec> specs = queryBus.ask(new ListAvailableLimitRulesQuery());
    ListLimitAssignmentsView assignments =
        queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.sellerTerminal(id)));
    ListLimitAssignmentsView inheritedAssignments =
        queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));
    return new LimitBundle(specs, assignments.items(), inheritedAssignments.items());
  }

  private TenantFinancialBreakdownView.SellerTerminalDailyFinancialRow todayStats(
      TchRequestContext ctx, SellerTerminalId id) {
    ZoneId tenantZone = ctx.tenantZoneId() == null ? ZoneOffset.UTC : ctx.tenantZoneId();
    LocalDate today = LocalDate.now(tenantZone);
    var breakdown =
        queryBus.ask(
            new GetTenantFinancialBreakdownQuery(
                ctx.tenantIdRequired(), today, today, 1, 1, List.of(), List.of(id.value())));
    if (breakdown == null || breakdown.sellerTerminalDailyRows() == null) {
      return null;
    }
    return breakdown.sellerTerminalDailyRows().stream()
        .filter(row -> id.value().equals(row.sellerTerminalId()))
        .findFirst()
        .orElse(null);
  }

  public record SellerTerminalDetailView(
      SellerTerminalView terminal,
      TenantSummary tenant,
      ClientDiagnosticPolicyView clientDiagnostics,
      TenantFinancialBreakdownView.SellerTerminalDailyFinancialRow todayStats,
      LimitBundle limits) {}

  public record TenantSummary(TenantId tenantId, String code, String name, String displayName) {}

  public record LimitBundle(
      List<LimitRuleSpec> specs,
      List<ListLimitAssignmentsView.Item> assignments,
      List<ListLimitAssignmentsView.Item> inheritedAssignments) {}
}
