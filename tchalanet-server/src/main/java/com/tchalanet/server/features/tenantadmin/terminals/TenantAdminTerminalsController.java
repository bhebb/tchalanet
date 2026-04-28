package com.tchalanet.server.features.tenantadmin.terminals;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.application.command.model.RegisterPosDeviceCommand;
import com.tchalanet.server.core.terminal.application.command.model.SendPosHeartbeatCommand;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.features.tenantadmin.terminals.model.TerminalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("${tch.web.paths.tenant_admin:/api/v1/tenant-admin}/terminals")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminTerminalsController {

    private final TenantAdminTerminalsOrchestrator orchestrator;

    @PostMapping
    public UUID registerDevice(@CurrentContext TchRequestContext ctx, @RequestBody RegisterPosDeviceCommand command) {
        return orchestrator.registerDevice(ctx, command);
    }

    @PostMapping("/{id}/heartbeat")
    public void sendHeartbeat(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody SendPosHeartbeatCommand command) {
        orchestrator.sendHeartbeat(ctx, id, command);
    }

    @PostMapping("/{id}/lock")
    public ApiResponse<TerminalResponse> lockDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody LockRequest request) {
        var res = orchestrator.lockDevice(ctx, id, request.actorId(), request.reason());
        if (res == null) return ApiResponse.success(null);
        return ApiResponse.success(TerminalResponse.fromDomain(res));
    }

    @PostMapping("/{id}/unlock")
    public ApiResponse<TerminalResponse> unlockDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody UnlockRequest request) {
        var res = orchestrator.unlockDevice(ctx, id, request.actorId());
        if (res == null) return ApiResponse.success(null);
        return ApiResponse.success(TerminalResponse.fromDomain(res));
    }

    @PutMapping("/{id}/metadata")
    public ApiResponse<TerminalResponse> updateMetadata(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody UpdateMetadataRequest request) {
        var res = orchestrator.updateMetadata(ctx, id, request.actorId(), request.metadataPatch(), request.heartbeatAlso());
        if (res == null) return ApiResponse.success(null);
        return ApiResponse.success(TerminalResponse.fromDomain(res));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<TerminalResponse> unregisterDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id, @RequestBody UnregisterRequest request) {
        var res = orchestrator.unregisterDevice(ctx, id, request.actorId(), request.reason());
        if (res == null) return ApiResponse.success(null);
        return ApiResponse.success(TerminalResponse.fromDomain(res));
    }

    @GetMapping("/{id}")
    public ApiResponse<TerminalResponse> getDevice(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id) {
        Optional<Terminal> opt = orchestrator.getDevice(ctx, id);
        if (opt.isEmpty()) return ApiResponse.success(null);
        return ApiResponse.success(TerminalResponse.fromDomain(opt.get()));
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> getDeviceStatus(@CurrentContext TchRequestContext ctx, @PathVariable TerminalId id) {
        return orchestrator.getDeviceStatus(ctx, id);
    }

    @GetMapping("/outlets/{outletId}")
    public ApiResponse<List<TerminalResponse>> listDevicesByOutlet(@CurrentContext TchRequestContext ctx, @PathVariable OutletId outletId) {
        var terminals = orchestrator.listDevicesByOutlet(ctx, outletId);
        var dtos = terminals.stream().map(TerminalResponse::fromDomain).toList();
        return ApiResponse.success(dtos);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<TerminalResponse>> listDevicesByTenant(@CurrentContext TchRequestContext ctx) {
        var terminals = orchestrator.listDevicesByTenant(ctx);
        var dtos = terminals.stream().map(TerminalResponse::fromDomain).toList();
        return ApiResponse.success(dtos);
    }

    // Request DTOs
    public record LockRequest(UUID actorId, String reason) {
    }

    public record UnlockRequest(UUID actorId) {
    }

    public record UpdateMetadataRequest(UUID actorId, Map<String, Object> metadataPatch, boolean heartbeatAlso) {
    }

    public record UnregisterRequest(UUID actorId, String reason) {
    }
}
