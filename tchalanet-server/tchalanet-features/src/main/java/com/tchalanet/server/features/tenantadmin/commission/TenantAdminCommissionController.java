package com.tchalanet.server.features.tenantadmin.commission;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.exception.TchBusinessRuleException;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.command.SetSellerTerminalCommissionRateCommand;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalCommissionStatsQuery;
import com.tchalanet.server.core.terminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.features.tenantadmin.commission.model.CommissionOverviewView;
import com.tchalanet.server.features.tenantadmin.commission.model.SellerTerminalCommissionRow;
import com.tchalanet.server.features.tenantadmin.commission.model.SetDefaultCommissionRateRequest;
import com.tchalanet.server.features.tenantadmin.commission.model.SetSellerTerminalCommissionRateRequest;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.internal.adapter.TenantPersistenceAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/commission")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Commission • Admin")
@Validated
public class TenantAdminCommissionController {

    private static final int SELLER_LIST_MAX = 500;

    private final QueryBus queryBus;
    private final CommandBus commandBus;
    private final TenantPreContextLookupApi tenantLookup;
    private final TenantPersistenceAdapter tenantPersistence;

    /**
     * Overview: tenant default commission rate + aggregate stats across all seller_terminals.
     */
    @GetMapping("/overview")
    public ApiResponse<CommissionOverviewView> overview(@CurrentContext TchRequestContext ctx) {
        TenantId tenantId = ctx.effectiveTenantIdRequired();
        BigDecimal defaultRate = resolveDefaultRate(tenantId);

        var stats = queryBus.ask(new GetSellerTerminalCommissionStatsQuery(tenantId, defaultRate));

        return ApiResponse.success(new CommissionOverviewView(
            defaultRate,
            stats.totalCount(),
            stats.countAtDefaultRate(),
            stats.countWithCustomRate(),
            stats.minRate(),
            stats.maxRate()));
    }

    /**
     * Set the tenant-wide default commission rate.
     * Existing seller_terminal rates are NOT touched; they keep their explicit rates.
     */
    @PutMapping("/default-rate")
    public ApiResponse<Void> setDefaultRate(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody SetDefaultCommissionRateRequest req
    ) {
        tenantPersistence.updateDefaultCommissionRate(ctx.effectiveTenantIdRequired(), req.rate());
        return ApiResponse.success(null);
    }

    /**
     * List all seller_terminals with their resolved commission rate and source.
     * Optional {@code status} filter (ACTIVE, PENDING, BLOCKED, DISABLED).
     */
    @GetMapping("/sellers")
    public ApiResponse<List<SellerTerminalCommissionRow>> listSellerCommissions(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        TenantId tenantId = ctx.effectiveTenantIdRequired();
        BigDecimal defaultRate = resolveDefaultRate(tenantId);

        int safeSize = Math.min(size, SELLER_LIST_MAX);
        TchPage<SellerTerminalSummaryRow> pageResult = queryBus.ask(new ListSellerTerminalsQuery(
            tenantId,
            SellerTerminalSearchCriteria.empty(),
            new TchPageRequest(PageRequest.of(page, safeSize))));

        List<SellerTerminalCommissionRow> rows = pageResult.items().stream()
            .map(row -> toCommissionRow(row, defaultRate))
            .toList();

        return ApiResponse.success(rows);
    }

    /**
     * Set a custom commission rate for a specific seller_terminal.
     */
    @PutMapping("/sellers/{sellerTerminalId}")
    public ApiResponse<Void> setSellerRate(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId,
        @Valid @RequestBody SetSellerTerminalCommissionRateRequest req
    ) {
        commandBus.execute(new SetSellerTerminalCommissionRateCommand(
            ctx.effectiveTenantIdRequired(),
            sellerTerminalId,
            req.rate(),
            ctx.userId()));
        return ApiResponse.success(null);
    }

    /**
     * Reset a seller_terminal's commission rate back to the tenant default.
     * If no tenant default is set, returns 422 (Unprocessable).
     */
    @DeleteMapping("/sellers/{sellerTerminalId}/custom-rate")
    public ApiResponse<Void> resetToDefault(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId
    ) {
        TenantId tenantId = ctx.effectiveTenantIdRequired();
        BigDecimal defaultRate = resolveDefaultRate(tenantId);
        if (defaultRate == null) {
            throw new TchBusinessRuleException(
                "tenant.commission.default_rate_not_set",
                "tenant.commission.default_rate_not_set");
        }
        commandBus.execute(new SetSellerTerminalCommissionRateCommand(
            tenantId,
            sellerTerminalId,
            defaultRate,
            ctx.userId()));
        return ApiResponse.success(null);
    }

    // ── helpers ──

    private BigDecimal resolveDefaultRate(TenantId tenantId) {
        return tenantLookup.findById(tenantId)
            .flatMap(v -> v.defaultCommissionRate())
            .orElse(null);
    }

    private SellerTerminalCommissionRow toCommissionRow(
        SellerTerminalSummaryRow row, BigDecimal defaultRate
    ) {
        SellerTerminalCommissionRow.CommissionRateSource source =
            (defaultRate != null && row.commissionRate() != null && defaultRate.compareTo(row.commissionRate()) == 0)
                ? SellerTerminalCommissionRow.CommissionRateSource.DEFAULT
                : SellerTerminalCommissionRow.CommissionRateSource.CUSTOM;

        return new SellerTerminalCommissionRow(
            row.id(),
            row.terminalCode(),
            row.displayName(),
            row.status(),
            row.commissionRate(),
            source);
    }
}
