package com.tchalanet.server.core.terminal.internal.infra.web.admin;

import com.tchalanet.server.catalog.plan.api.PlanFeatureKeys;
import com.tchalanet.server.catalog.plan.api.PlanLimitKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.api.command.LockTerminalCommand;
import com.tchalanet.server.core.terminal.api.command.RegisterTerminalCommand;
import com.tchalanet.server.core.terminal.api.command.UnlockTerminalCommand;
import com.tchalanet.server.core.terminal.api.command.UnregisterTerminalCommand;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.RegisterTerminalRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.TerminalLockRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.UnregisterTerminalRequest;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import com.tchalanet.server.platform.entitlement.api.RequiredQuota;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Terminal • Admin Lifecycle")
@RequiredArgsConstructor
public class TerminalAdminLifecycleController {

    private final CommandBus commandBus;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiredFeature(PlanFeatureKeys.TERMINAL_LICENSING)
    @RequiredQuota(
        limit = PlanLimitKeys.TERMINALS_MAX,
        usage = UsageKeys.TERMINALS_ACTIVE
    )
    @RequiresPermission("terminal.create")
    @Operation(summary = "Create a tenant terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_REGISTER,
        idExpression = "#result.data.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<TerminalId> create(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody RegisterTerminalRequest request
    ) {
        var id = commandBus.execute(new RegisterTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            request.outletId(),
            request.kind() == null ? TerminalKind.PHYSICAL : request.kind(),
            request.label(),
            request.inventoryTag(),
            request.metadata(),
            ctx.currentUserIdRequired()));

        return ApiResponse.created(id);
    }

    @DeleteMapping("/{terminalId}")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.retire")
    @Operation(summary = "Retire a tenant terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_UNREGISTER,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#request")
    public void retire(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody UnregisterTerminalRequest request
    ) {
        commandBus.execute(new UnregisterTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            request.reason(),
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{terminalId}/lock")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.lock")
    @Operation(summary = "Lock a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_LOCK,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#request")
    public void lock(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody TerminalLockRequest request
    ) {
        commandBus.execute(new LockTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            request.reason(),
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{terminalId}/unlock")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.unlock")
    @Operation(summary = "Unlock a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_UNLOCK,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#request")
    public void unlock(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody TerminalLockRequest request
    ) {
        commandBus.execute(new UnlockTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            request.reason(),
            ctx.currentUserIdRequired()));
    }
}
