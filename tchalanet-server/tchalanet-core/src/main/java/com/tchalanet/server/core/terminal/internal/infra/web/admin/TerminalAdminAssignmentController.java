package com.tchalanet.server.core.terminal.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.api.command.ActivateTerminalForUserCommand;
import com.tchalanet.server.core.terminal.api.command.AssignTerminalToOutletCommand;
import com.tchalanet.server.core.terminal.api.command.AssignTerminalToUserCommand;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.AssignOutletRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.AssignUserRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Terminal • Admin Assignments")
@RequiredArgsConstructor
public class TerminalAdminAssignmentController {

    private final CommandBus commandBus;

    @PostMapping("/{terminalId}/assign-outlet")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.assign.outlet")
    @Operation(summary = "Assign an outlet to a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_ASSIGN_OUTLET,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<Void> assignOutlet(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody AssignOutletRequest req) {
        commandBus.execute(
            new AssignTerminalToOutletCommand(
                ctx.tenantIdSafe(), terminalId, req.outletId(), ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{terminalId}/assign-user")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.assign.user")
    @Operation(summary = "Assign a user to a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_ASSIGN_USER,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<Void> assignUser(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody AssignUserRequest req) {
        commandBus.execute(
            new AssignTerminalToUserCommand(
                ctx.tenantIdSafe(), terminalId, req.userId(), ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{terminalId}/activate-for-user")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.activate.user")
    @Operation(summary = "Activate a terminal for the current user")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_ACTIVATE_FOR_USER,
        idExpression = "#terminalId.value().toString()")
    public ApiResponse<Void> activateForUser(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId) {
        commandBus.execute(
            new ActivateTerminalForUserCommand(
                ctx.tenantIdSafe(), terminalId, ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }
}
