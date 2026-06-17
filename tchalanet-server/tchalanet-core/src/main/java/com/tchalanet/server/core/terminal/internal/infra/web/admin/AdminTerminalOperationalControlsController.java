package com.tchalanet.server.core.terminal.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.api.command.SetTerminalOperationalControlCommand;
import com.tchalanet.server.core.terminal.api.command.TerminalOperationalControl;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.SetOperationalControlRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/terminals/{terminalId}/operational-controls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Terminal • Admin Operational Controls")
public class AdminTerminalOperationalControlsController {

    private final CommandBus commandBus;

    @PatchMapping("/{control}")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.control.set")
    @Operation(summary = "Enable or disable an operational control on a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_OPERATIONAL_CONTROL_SET,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#request")
    public void setControl(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @PathVariable TerminalOperationalControl control,
        @Valid @RequestBody SetOperationalControlRequest request) {
        commandBus.execute(new SetTerminalOperationalControlCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            control,
            request.blocked(),
            request.reason(),
            ctx.currentUserIdRequired()));
    }
}
