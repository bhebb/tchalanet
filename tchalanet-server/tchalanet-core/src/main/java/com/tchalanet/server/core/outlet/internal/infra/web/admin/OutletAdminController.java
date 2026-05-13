package com.tchalanet.server.core.outlet.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.outlet.api.command.BlockOutletSalesCommand;
import com.tchalanet.server.core.outlet.api.command.CloseOutletDayCommand;
import com.tchalanet.server.core.outlet.api.command.CloseOutletDayPayload;
import com.tchalanet.server.core.outlet.api.command.CreateOutletCommand;
import com.tchalanet.server.core.outlet.api.command.OutletConfigPatch;
import com.tchalanet.server.core.outlet.api.command.UnblockOutletSalesCommand;
import com.tchalanet.server.core.outlet.api.command.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.BlockSalesRequest;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.CloseOutletDayRequest;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.CreateOutletRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/outlets")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Outlet • Admin")
public class OutletAdminController {

    private final CommandBus commandBus;

    @PostMapping
    @AuditLog(
        entity = AuditEntityType.OUTLET,
        action = AuditAction.OUTLET_CREATE,
        idExpression = "#result.data.toString()",
        detailsExpression = "#req")
    public ApiResponse<OutletId> create(
        @CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateOutletRequest req) {
        return ApiResponse.success(
            commandBus.execute(
                new CreateOutletCommand(ctx.tenantIdSafe(), req.name(), req.slug(), null, req.address())));
    }

    @PatchMapping("/{id}/config")
    @AuditLog(
        entity = AuditEntityType.OUTLET,
        action = AuditAction.OUTLET_UPDATE,
        idExpression = "#id.value().toString()",
        detailsExpression = "#patch")
    public ApiResponse<Void> updateConfig(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @RequestBody OutletConfigPatch patch) {
        commandBus.execute(new UpdateOutletConfigCommand(ctx.tenantIdSafe(), id, patch));
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/block-sales")
    @AuditLog(
        entity = AuditEntityType.OUTLET,
        action = AuditAction.OUTLET_BLOCK_SALES,
        idExpression = "#id.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<Void> blockSales(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @Valid @RequestBody BlockSalesRequest req) {
        commandBus.execute(
            new BlockOutletSalesCommand(
                ctx.tenantIdSafe(), id, req.reason(), ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }


    @PostMapping("/{id}/unblock-sales")
    @AuditLog(
        entity = AuditEntityType.OUTLET,
        action = AuditAction.OUTLET_UNBLOCK_SALES,
        idExpression = "#id.value().toString()")
    public ApiResponse<Void> unblockSales(
        @CurrentContext TchRequestContext ctx, @PathVariable OutletId id) {
        commandBus.execute(
            new UnblockOutletSalesCommand(ctx.tenantIdSafe(), id, ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/close-day")
    public ApiResponse<Void> closeDay(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @Valid @RequestBody CloseOutletDayRequest req) {
        var outletDayPayload = new CloseOutletDayPayload(req.from(), req.to(), req.mode(), req.reason());
        var cmd = new CloseOutletDayCommand(ctx.tenantIdSafe(), id, outletDayPayload, ctx.currentUserIdRequired());
        var result = commandBus.execute(cmd);

        return ApiResponse.success(result);
    }

}
