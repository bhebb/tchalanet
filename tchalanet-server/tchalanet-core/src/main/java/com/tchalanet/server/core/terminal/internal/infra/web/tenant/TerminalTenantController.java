package com.tchalanet.server.core.terminal.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.terminal.application.command.model.SendTerminalHeartbeatCommand;
import com.tchalanet.server.core.terminal.application.query.model.GetCurrentTerminalQuery;
import com.tchalanet.server.core.terminal.application.query.model.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.infra.web.tenant.model.SyncStateRequest;
import com.tchalanet.server.core.terminal.infra.web.tenant.model.TerminalResponse;
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

/**
 * Tenant-scoped runtime endpoints for terminals: heartbeat, current terminal lookup, status read,
 * and sync state report. Each request must come from an authenticated tenant user; RLS isolates
 * data.
 */
@RestController
@RequestMapping("/tenant/terminals")
@PreAuthorize("hasAnyAuthority('CASHIER','SUPERVISOR','TENANT_ADMIN')")
@RequiredArgsConstructor
public class TerminalTenantController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final Clock clock;
    private final TerminalTenantWebMapper mapper;

    @GetMapping("/current")
    public ApiResponse<TerminalResponse> current(@CurrentContext TchRequestContext ctx) {
        var view = queryBus.ask(new GetCurrentTerminalQuery(ctx.currentUserIdRequired()));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<TerminalResponse> status(@CurrentContext TchRequestContext ctx,
                                                @PathVariable TerminalId id) {
        var view = queryBus.ask(new GetTerminalByIdQuery(ctx.effectiveTenantIdRequired(), id));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @PostMapping("/{id}/heartbeat")
    @ResponseStatus(HttpStatus.OK)
    public void heartbeat(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId id) {
        commandBus.execute(new SendTerminalHeartbeatCommand(ctx.tenantIdSafe(), id, Instant.now(clock)));
    }

    @PostMapping("/{id}/sync-state")
    @ResponseStatus(HttpStatus.OK)
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_SYNC_STATE_UPDATE,
        idExpression = "#id.value().toString()",
        detailsExpression = "#req")
    public void reportSyncState(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId id,
        @Valid @RequestBody SyncStateRequest req) {
        commandBus.execute(new SendTerminalHeartbeatCommand(ctx.tenantIdSafe(), id, Instant.now(clock)));

    }
}
