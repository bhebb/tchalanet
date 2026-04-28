package com.tchalanet.server.core.terminal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.application.command.model.*;
import com.tchalanet.server.core.terminal.application.query.model.*;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TerminalAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ApiResponse<TerminalId> registerDevice(@CurrentContext TchRequestContext ctx, @RequestBody RegisterPosDeviceCommand body) {
        var cmd = new RegisterPosDeviceCommand(ctx.tenantIdSafe(), body.outletId(), body.deviceId(), body.label(), body.capabilities());
        return ApiResponse.success(TerminalId.of(commandBus.send(cmd)));
    }

    @PostMapping("/{id}/heartbeat")
    public ApiResponse<Void> sendHeartbeat(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody SendPosHeartbeatCommand body) {
        var cmd = new SendPosHeartbeatCommand(ctx.tenantIdSafe(), id, body.lastSeenAt(), body.status(), body.batteryPercent(), body.appVersion(), body.extras());
        commandBus.send(cmd);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/lock")
    public ApiResponse<TerminalResponse> lockDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody LockRequest req) {
        var cmd = new LockTerminalCommand(ctx.tenantIdSafe(), id, req.actorId().value(), req.reason());
        Terminal res = commandBus.send(cmd);
        return ApiResponse.success(res == null ? null : TerminalResponse.fromDomain(res));
    }

    @PostMapping("/{id}/unlock")
    public ApiResponse<TerminalResponse> unlockDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody UnlockRequest req) {
        var cmd = new UnlockTerminalCommand(ctx.tenantIdSafe(), id, req.actorId().value());
        Terminal res = commandBus.send(cmd);
        return ApiResponse.success(res == null ? null : TerminalResponse.fromDomain(res));
    }

    @PutMapping("/{id}/metadata")
    public ApiResponse<TerminalResponse> updateMetadata(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody UpdateMetadataRequest req) {
        var cmd = new UpdateTerminalMetadataCommand(ctx.tenantIdSafe(), id, req.actorId().value(), req.metadataPatch(), req.heartbeatAlso());
        Terminal res = commandBus.send(cmd);
        return ApiResponse.success(res == null ? null : TerminalResponse.fromDomain(res));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<TerminalResponse> unregisterDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody UnregisterRequest req) {
        var cmd = new UnregisterTerminalCommand(ctx.tenantIdSafe(), id, req.actorId().value(), req.reason());
        Terminal res = commandBus.send(cmd);
        return ApiResponse.success(res == null ? null : TerminalResponse.fromDomain(res));
    }

    @GetMapping("/{id}")
    public ApiResponse<TerminalResponse> getDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id) {
        Optional<Terminal> opt = queryBus.send(new GetPosDeviceByIdQuery(ctx.tenantIdSafe(), id));
        return ApiResponse.success(opt.map(TerminalResponse::fromDomain).orElse(null));
    }

    @GetMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Map<String, Object>> getDeviceStatus(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id) {
        return ApiResponse.success(queryBus.send(new GetPosDeviceStatusQuery(ctx.tenantIdSafe(), id)));
    }

    @GetMapping("/outlets/{outletId}")
    public ApiResponse<List<TerminalResponse>> listByOutlet(@CurrentContext TchRequestContext ctx, @PathVariable OutletId outletId) {
        List<Terminal> terminals = queryBus.send(new ListPosDevicesByLocationQuery(ctx.tenantIdSafe(), outletId));
        return ApiResponse.success(terminals.stream().map(TerminalResponse::fromDomain).toList());
    }

    @GetMapping
    public ApiResponse<List<TerminalResponse>> listByTenant(@CurrentContext TchRequestContext ctx) {
        List<Terminal> terminals = queryBus.send(new ListPosDevicesByTenantQuery(ctx.tenantIdSafe()));
        return ApiResponse.success(terminals.stream().map(TerminalResponse::fromDomain).toList());
    }

    public record LockRequest(UserId actorId, String reason) {}
    public record UnlockRequest(UserId actorId) {}
    public record UpdateMetadataRequest(UserId actorId, Map<String, Object> metadataPatch, boolean heartbeatAlso) {}
    public record UnregisterRequest(UserId actorId, String reason) {}

    public record TerminalResponse(java.util.UUID id, java.util.UUID outletId, String label, String status) {
        public static TerminalResponse fromDomain(Terminal t) {
            return new TerminalResponse(t.id(), t.outletId() == null ? null : t.outletId().value(), t.label(), t.state() != null ? t.state().name() : null);
        }
    }
}
