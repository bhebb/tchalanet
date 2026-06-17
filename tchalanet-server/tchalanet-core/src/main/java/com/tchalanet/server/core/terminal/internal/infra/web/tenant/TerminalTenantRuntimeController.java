package com.tchalanet.server.core.terminal.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.api.command.SendTerminalHeartbeatCommand;
import com.tchalanet.server.core.terminal.api.command.UpdateTerminalSyncStateCommand;
import com.tchalanet.server.core.terminal.api.query.GetCurrentTerminalQuery;
import com.tchalanet.server.core.terminal.api.query.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.SyncStateRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.TerminalResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/tenant/terminals")
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
@RequiredArgsConstructor
@Tag(name = "Terminal • Tenant Runtime")
public class TerminalTenantRuntimeController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final Clock clock;
    private final TerminalTenantWebMapper mapper;

    @GetMapping("/current")
    @Operation(summary = "Get the current terminal for the authenticated user")
    public ApiResponse<TerminalResponse> current(@CurrentContext TchRequestContext ctx) {
        var view = queryBus.ask(new GetCurrentTerminalQuery(ctx.currentUserIdRequired()));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @GetMapping("/{terminalId}/status")
    @Operation(summary = "Get the status of a terminal by ID")
    public ApiResponse<TerminalResponse> status(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId) {
        var view = queryBus.ask(new GetTerminalByIdQuery(ctx.effectiveTenantIdRequired(), terminalId));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @PostMapping("/{terminalId}/heartbeat")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Send a heartbeat for a terminal")
    public void heartbeat(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId) {
        commandBus.execute(new SendTerminalHeartbeatCommand(ctx.tenantIdSafe(), terminalId, Instant.now(clock)));
    }

    @PostMapping("/{terminalId}/sync-state")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Report the current sync state of a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_SYNC_STATE_UPDATE,
        idExpression = "#terminalId.value().toString()",
        detailsExpression = "#req")
    public void reportSyncState(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody SyncStateRequest req) {
        commandBus.execute(new UpdateTerminalSyncStateCommand(
            ctx.tenantIdSafe(), terminalId, req.newSyncState(), ctx.currentUserIdRequired()));
    }
}
