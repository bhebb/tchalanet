package com.tchalanet.server.core.pricing.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.pricing.api.command.DeactivateSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.DeleteSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalPricingRuleOverrideResult;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantPricingRuleCommand;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalPricingRuleOverrideView;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import com.tchalanet.server.core.pricing.api.query.ListSellerTerminalPricingRuleOverridesQuery;
import com.tchalanet.server.core.pricing.api.query.ListTenantPricingRulesQuery;
import com.tchalanet.server.core.pricing.internal.infra.web.admin.model.UpsertPricingRuleOverrideRequest;
import com.tchalanet.server.core.pricing.internal.infra.web.admin.model.UpsertTenantPricingRuleRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/controls/pricing-rules")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Pricing • Admin")
@Validated
public class PricingOverrideAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /** Tenant-wide default pricing rules. */
    @GetMapping
    public ApiResponse<List<TenantPricingRuleView>> listTenantPricingRules(
        @CurrentContext TchRequestContext ctx
    ) {
        return ApiResponse.success(queryBus.ask(new ListTenantPricingRulesQuery(ctx.tenantIdRequired(), null)));
    }

    /** Create or update a tenant default pricing rule. */
    @PutMapping
    public ApiResponse<TenantPricingRuleView> upsertTenantDefaultPricing(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody UpsertTenantPricingRuleRequest req
    ) {
        var cmd = new UpsertTenantPricingRuleCommand(
            ctx.tenantIdRequired(),
            req.gameCode(),
            req.pricingVariantCode(),
            req.betType(),
            req.betOption(),
            req.odds(),
            req.payoutRuleType(),
            req.fixedAmount(),
            ctx.userId());
        return ApiResponse.success(commandBus.execute(cmd));
    }

    /** Active seller-terminal pricing rule overrides. */
    @GetMapping("/seller-terminals/{sellerTerminalId}")
    public ApiResponse<List<SellerTerminalPricingRuleOverrideView>> listOverrides(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId
    ) {
        var result = queryBus.ask(new ListSellerTerminalPricingRuleOverridesQuery(
            ctx.tenantIdRequired(), sellerTerminalId));
        return ApiResponse.success(result);
    }

    /** Create or update a seller-terminal pricing rule override (upsert by natural key). */
    @PutMapping("/seller-terminals/{sellerTerminalId}")
    public ApiResponse<UpsertSellerTerminalPricingRuleOverrideResult> upsertOverride(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId,
        @Valid @RequestBody UpsertPricingRuleOverrideRequest req
    ) {
        var cmd = new UpsertSellerTerminalPricingRuleOverrideCommand(
            ctx.tenantIdRequired(), sellerTerminalId,
            req.gameCode(), req.pricingVariantCode(), req.betType(), req.betOption(),
            req.odds(), req.payoutRuleType(), req.fixedAmount(), req.effectiveFrom(), req.effectiveTo(),
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
        commandBus.execute(new DeleteSellerTerminalPricingRuleOverrideCommand(overrideId, ctx.userId()));
        return ApiResponse.success(null);
    }

    /** Deactivate (soft-disable) a specific override without deleting it. */
    @PostMapping("/seller-terminals/{sellerTerminalId}/overrides/{overrideId}/deactivate")
    public ApiResponse<Void> deactivateOverride(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId sellerTerminalId,
        @PathVariable SellerTerminalOddsOverrideId overrideId
    ) {
        commandBus.execute(new DeactivateSellerTerminalPricingRuleOverrideCommand(overrideId, ctx.userId()));
        return ApiResponse.success(null);
    }
}
