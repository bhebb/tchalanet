package com.tchalanet.server.features.tenantadmin.pricing;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.pricing.api.command.DeleteTenantOddsCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantOddsCommand;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.TenantPricingOddsView;
import com.tchalanet.server.core.pricing.api.query.ListTenantPricingQuery;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tenant Admin • Pricing")
@RestController
@RequestMapping("/admin/pricing/odds")
@PreAuthorize("hasPermission(null, 'game-pricing.update')")
@RequiredArgsConstructor
@Validated
public class TenantAdminPricingController {

    private final QueryBus queryBus;
    private final CommandBus commandBus;

    @Operation(summary = "List tenant default odds")
    @GetMapping
    public ApiResponse<List<TenantPricingOddsView>> list(
        @RequestParam(required = false) String gameCode,
        @CurrentContext TchRequestContext ctx
    ) {
        return ApiResponse.success(queryBus.ask(new ListTenantPricingQuery(ctx.tenantIdRequired(), gameCode)));
    }

    @Operation(summary = "Create or update a tenant default odds by pricing variant")
    @PutMapping
    @AuditLog(
        entity = AuditEntityType.ODDS,
        action = AuditAction.UPDATE,
        idExpression = "#req.gameCode() + ':' + #req.pricingVariantCode().name()",
        detailsExpression = "#req")
    public ApiResponse<TenantPricingOddsView> upsert(
        @Valid @RequestBody UpsertTenantOddsRequest req,
        @CurrentContext TchRequestContext ctx
    ) {
        var result = commandBus.execute(new UpsertTenantOddsCommand(
            ctx.tenantIdRequired(),
            req.gameCode(),
            req.pricingVariantCode(),
            req.betType(),
            req.betOption(),
            req.odds(),
            ctx.userId()
        ));
        return ApiResponse.success(result);
    }

    @Operation(summary = "Delete a tenant default odds by pricing variant")
    @DeleteMapping
    @AuditLog(
        entity = AuditEntityType.ODDS,
        action = AuditAction.DELETE,
        idExpression = "#req.gameCode() + ':' + #req.pricingVariantCode().name()",
        detailsExpression = "#req")
    public ApiResponse<Void> delete(
        @Valid @RequestBody DeleteTenantOddsRequestBody req,
        @CurrentContext TchRequestContext ctx
    ) {
        commandBus.execute(new DeleteTenantOddsCommand(
            ctx.tenantIdRequired(),
            req.gameCode(),
            req.pricingVariantCode(),
            ctx.userId()
        ));
        return ApiResponse.success(null);
    }

    public record UpsertTenantOddsRequest(
        @NotBlank String gameCode,
        @NotNull PricingVariantCode pricingVariantCode,
        @NotBlank String betType,
        Short betOption,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal odds
    ) {}

    public record DeleteTenantOddsRequestBody(
        @NotBlank String gameCode,
        @NotNull PricingVariantCode pricingVariantCode
    ) {}
}
