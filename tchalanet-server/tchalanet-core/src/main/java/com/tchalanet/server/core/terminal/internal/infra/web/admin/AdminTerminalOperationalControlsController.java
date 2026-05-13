package com.tchalanet.server.core.terminal.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.terminal.api.command.LockTerminalCommand;
import com.tchalanet.server.core.terminal.api.command.RegisterTerminalCommand;
import com.tchalanet.server.core.terminal.api.command.SetTerminalOperationalControlCommand;
import com.tchalanet.server.core.terminal.api.command.TerminalOperationalControl;
import com.tchalanet.server.core.terminal.api.command.UnlockTerminalCommand;
import com.tchalanet.server.core.terminal.api.command.UnregisterTerminalCommand;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.RegisterTerminalRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.SetOperationalControlRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.TerminalLockRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.UnregisterTerminalRequest;
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
@RequestMapping("/admin/terminals/{terminalId}/operational-controls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Terminal • Admin Operational Controls")

public class AdminTerminalOperationalControlsController {

    private final CommandBus commandBus;


    @PostMapping
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_REGISTER,
        idExpression = "#result.data.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<TerminalId> register(
        @CurrentContext TchRequestContext ctx, @Valid @RequestBody RegisterTerminalRequest req) {
        return ApiResponse.success(
            commandBus.execute(
                new RegisterTerminalCommand(
                    ctx.tenantIdSafe(),
                    req.outletId(),
                    req.kind() == null ? TerminalKind.PHYSICAL : req.kind(),
                    req.label(),
                    req.inventoryTag(),
                    req.metadata(),
                    ctx.currentUserIdRequired())));
    }

    @DeleteMapping("/{id}")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_UNREGISTER,
        idExpression = "#id.value().toString()",
        detailsExpression = "#req")
    @ResponseStatus(HttpStatus.OK)
    public void unregister(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId id,
        @Valid @RequestBody UnregisterTerminalRequest req) {
        commandBus.execute(
            new UnregisterTerminalCommand(
                ctx.tenantIdSafe(), id, req.reason(), ctx.currentUserIdRequired()));
    }

    @PatchMapping("/lock")
    @PreAuthorize("hasPermission(#terminalId, 'TERMINAL_LOCK')")
    @ResponseStatus(HttpStatus.OK)
    public void lock(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody TerminalLockRequest request) {
        commandBus.execute(new LockTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            request.reason(),
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/unlock")
    @PreAuthorize("hasPermission(#terminalId, 'TERMINAL_UNLOCK')")
    @ResponseStatus(HttpStatus.OK)
    public void unlock(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody TerminalLockRequest request) {
        commandBus.execute(new UnlockTerminalCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            request.reason(),
            ctx.currentUserIdRequired()));
    }

    @PatchMapping("/{control}")
    @PreAuthorize("hasPermission(#terminalId, 'TERMINAL_OPERATIONAL_CONTROL_UPDATE')")
    @ResponseStatus(HttpStatus.OK)
    public void setControl(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @PathVariable TerminalOperationalControl control,
        @Valid @RequestBody SetOperationalControlRequest request) {
        commandBus.execute(new SetTerminalOperationalControlCommand(
            terminalId,
            control,
            request.blocked(),
            request.reason(),
            ctx.currentUserIdRequired()));
    }
}
