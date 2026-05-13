package com.tchalanet.server.core.terminal.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.terminal.api.command.ActivateTerminalForUserCommand;
import com.tchalanet.server.core.terminal.api.command.AssignTerminalToOutletCommand;
import com.tchalanet.server.core.terminal.api.command.AssignTerminalToUserCommand;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.AssignOutletRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.AssignUserRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Terminal • Admin Assignments")
@RequiredArgsConstructor
public class TerminalAdminAssignmentController {

    private final CommandBus commandBus;


    @PostMapping("/{id}/assign-outlet")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_ASSIGN_OUTLET,
        idExpression = "#id.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<Void> assignOutlet(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId id,
        @Valid @RequestBody AssignOutletRequest req) {
        commandBus.execute(
            new AssignTerminalToOutletCommand(
                ctx.tenantIdSafe(), id, req.outletId(), ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/assign-user")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_ASSIGN_USER,
        idExpression = "#id.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<Void> assignUser(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId id,
        @Valid @RequestBody AssignUserRequest req) {
        commandBus.execute(
            new AssignTerminalToUserCommand(
                ctx.tenantIdSafe(), id, req.userId(), ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/activate-for-user")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_ACTIVATE_FOR_USER,
        idExpression = "#id.value().toString()")
    public ApiResponse<Void> activateForUser(
        @CurrentContext TchRequestContext ctx, @PathVariable TerminalId id) {
        commandBus.execute(
            new ActivateTerminalForUserCommand(
                ctx.tenantIdSafe(), id, ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }
}
