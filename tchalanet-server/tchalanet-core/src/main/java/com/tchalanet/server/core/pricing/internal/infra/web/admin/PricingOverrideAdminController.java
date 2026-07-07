package com.tchalanet.server.core.pricing.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.pricing.api.command.DeactivateSellerTerminalOddsOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.DeleteSellerTerminalOddsOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalOddsOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalOddsOverrideResult;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsOverrideView;
import com.tchalanet.server.core.pricing.api.model.TenantPricingOddsView;
import com.tchalanet.server.core.pricing.api.query.ListSellerTerminalOddsOverridesQuery;
import com.tchalanet.server.core.pricing.api.query.ListTenantPricingQuery;
import com.tchalanet.server.core.pricing.internal.infra.web.admin.model.UpsertOddsOverrideRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/controls/odds")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Pricing • Admin")
@Validated
public class PricingOverrideAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /** Tenant-wide default odds from the catalog (all games × betTypes). */
    @GetMapping
    public ApiResponse<List<TenantPricingOddsView>> getTenantDefaultOdds(
        @CurrentContext TchRequestContext ctx
    ) {
        return ApiResponse.success(queryBus.ask(new ListTenantPricingQuery(ctx.tenantIdRequired(), null)));
    }

    /** Active overrides for a seller_terminal, merged with tenant defaults via ResolveSellerTerminalOddsQuery. */
    @GetMapping("/seller-terminals/{sellerTerminalId}")
    public ApiResponse<List<SellerTerminalOddsOverrideView>> listOverrides(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId
    ) {
        var result = queryBus.ask(new ListSellerTerminalOddsOverridesQuery(
            ctx.tenantIdRequired(), sellerTerminalId));
        return ApiResponse.success(result);
    }

    /** Create or update a seller_terminal odds override (upsert by natural key). */
    @PutMapping("/seller-terminals/{sellerTerminalId}")
    public ApiResponse<UpsertSellerTerminalOddsOverrideResult> upsertOverride(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId,
        @Valid @RequestBody UpsertOddsOverrideRequest req
    ) {
        var cmd = new UpsertSellerTerminalOddsOverrideCommand(
            ctx.tenantIdRequired(), sellerTerminalId,
            req.gameCode(), req.pricingVariantCode(), req.betType(), req.betOption(),
            req.odds(), req.effectiveFrom(), req.effectiveTo(),
            req.reason(), ctx.userId());
        return ApiResponse.success(commandBus.execute(cmd));
    }

    /** Hard-delete a specific override (falls back to tenant default). */
    @DeleteMapping("/seller-terminals/{sellerTerminalId}/overrides/{overrideId}")
    public ApiResponse<Void> deleteOverride(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId,
        @PathVariable SellerTerminalOddsOverrideId overrideId
    ) {
        commandBus.execute(new DeleteSellerTerminalOddsOverrideCommand(overrideId, ctx.userId()));
        return ApiResponse.success(null);
    }

    /** Deactivate (soft-disable) a specific override without deleting it. */
    @PostMapping("/seller-terminals/{sellerTerminalId}/overrides/{overrideId}/deactivate")
    public ApiResponse<Void> deactivateOverride(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId,
        @PathVariable SellerTerminalOddsOverrideId overrideId
    ) {
        commandBus.execute(new DeactivateSellerTerminalOddsOverrideCommand(overrideId, ctx.userId()));
        return ApiResponse.success(null);
    }
}
