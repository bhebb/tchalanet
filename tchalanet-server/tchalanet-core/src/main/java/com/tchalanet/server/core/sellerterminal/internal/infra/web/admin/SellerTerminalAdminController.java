package com.tchalanet.server.core.sellerterminal.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.sellerterminal.api.command.BlockSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.api.command.CreateSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.api.command.DisableSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.api.command.ResetSellerTerminalAccessCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UnblockSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.sellerterminal.internal.infra.web.admin.model.BlockSellerTerminalRequest;
import com.tchalanet.server.core.sellerterminal.internal.infra.web.admin.model.CreateSellerTerminalRequest;
import com.tchalanet.server.core.sellerterminal.internal.infra.web.admin.model.UpdateSellerTerminalRequest;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seller-terminals")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Vendeurs / Terminaux • Admin")
@RequiredArgsConstructor
public class SellerTerminalAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    // ── Queries ───────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List seller terminals")
    public ApiResponse<TchPage<SellerTerminalSummaryRow>> list(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) SellerTerminalStatus status,
        @TchPaging(defaultSort = {"displayName,ASC"}, allowedSort = {"displayName", "terminalCode", "status", "createdAt"})
        TchPageRequest pageRequest
    ) {
        var criteria = new SellerTerminalSearchCriteria(q, status);
        return ApiResponse.success(queryBus.ask(
            new ListSellerTerminalsQuery(ctx.effectiveTenantIdRequired(), criteria, pageRequest)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get seller terminal by id")
    public ApiResponse<SellerTerminalView> get(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId id
    ) {
        return ApiResponse.success(queryBus.ask(
            new GetSellerTerminalQuery(ctx.effectiveTenantIdRequired(), id)));
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("seller_terminal.manage")
    @Operation(summary = "Create a seller terminal (Vendeur / Terminal)")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_CREATE,
        idExpression = "#result.data.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<SellerTerminalId> create(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CreateSellerTerminalRequest request
    ) {
        var id = commandBus.execute(new CreateSellerTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            request.terminalCode(),
            request.displayName(),
            request.firstName(),
            request.lastName(),
            request.phoneNumber(),
            request.addressId(),
            request.commissionRate(),
            request.initialPin(),
            ctx.currentUserIdRequired()));
        return ApiResponse.created(id);
    }

    @PutMapping("/{id}")
    @RequiresPermission("seller_terminal.manage")
    @Operation(summary = "Update a seller terminal")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_UPDATE,
        idExpression = "#id.value().toString()",
        detailsExpression = "#request")
    public void update(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId id,
        @Valid @RequestBody UpdateSellerTerminalRequest request
    ) {
        commandBus.execute(new UpdateSellerTerminalCommand(
            ctx.effectiveTenantIdRequired(), id,
            request.displayName(),
            request.firstName(),
            request.lastName(),
            request.phoneNumber(),
            request.addressId(),
            request.commissionRate(),
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{id}/block")
    @RequiresPermission("seller_terminal.block")
    @Operation(summary = "Block a seller terminal")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_BLOCK,
        idExpression = "#id.value().toString()",
        detailsExpression = "#request")
    public void block(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId id,
        @Valid @RequestBody BlockSellerTerminalRequest request
    ) {
        commandBus.execute(new BlockSellerTerminalCommand(
            ctx.effectiveTenantIdRequired(), id,
            request.reason(),
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{id}/unblock")
    @RequiresPermission("seller_terminal.block")
    @Operation(summary = "Unblock a seller terminal")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_UNBLOCK,
        idExpression = "#id.value().toString()")
    public void unblock(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId id
    ) {
        commandBus.execute(new UnblockSellerTerminalCommand(
            ctx.effectiveTenantIdRequired(), id,
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{id}/disable")
    @RequiresPermission("seller_terminal.manage")
    @Operation(summary = "Disable a seller terminal permanently")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_DISABLE,
        idExpression = "#id.value().toString()")
    public void disable(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId id
    ) {
        commandBus.execute(new DisableSellerTerminalCommand(
            ctx.effectiveTenantIdRequired(), id,
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{id}/reset-access")
    @RequiresPermission("seller_terminal.reset_access")
    @Operation(summary = "Reset PIN for a seller terminal")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_RESET_ACCESS,
        idExpression = "#id.value().toString()")
    public void resetAccess(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SellerTerminalId id,
        @Valid @RequestBody ResetPinRequest request
    ) {
        commandBus.execute(new ResetSellerTerminalAccessCommand(
            ctx.effectiveTenantIdRequired(), id,
            request.newPin(),
            ctx.currentUserIdRequired()));
    }

    public record ResetPinRequest(
        @jakarta.validation.constraints.Pattern(
            regexp = "\\d{4,8}", message = "PIN must be 4–8 digits")
        String newPin) {}
}
