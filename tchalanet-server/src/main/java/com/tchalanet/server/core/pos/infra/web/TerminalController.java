package com.tchalanet.server.core.pos.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.pos.application.command.model.LockTerminalCommand;
import com.tchalanet.server.core.pos.application.command.model.RegisterPosDeviceCommand;
import com.tchalanet.server.core.pos.application.command.model.SendPosHeartbeatCommand;
import com.tchalanet.server.core.pos.application.command.model.UnlockTerminalCommand;
import com.tchalanet.server.core.pos.application.command.model.UnregisterTerminalCommand;
import com.tchalanet.server.core.pos.application.command.model.UpdateTerminalMetadataCommand;
import com.tchalanet.server.core.pos.application.query.model.GetPosDeviceByIdQuery;
import com.tchalanet.server.core.pos.application.query.model.GetPosDeviceStatusQuery;
import com.tchalanet.server.core.pos.application.query.model.ListPosDevicesByLocationQuery;
import com.tchalanet.server.core.pos.application.query.model.ListPosDevicesByTenantQuery;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.infra.web.model.TerminalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Admin • Terminals",
    description = "Tenant administration of POS terminals: register, lock/unlock, metadata")
@RestController
@RequestMapping("/admin/tenants/{tenantId}/terminals")
@RequiredArgsConstructor
public class TerminalController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  public UUID registerDevice(
      @PathVariable TenantId tenantId, @RequestBody RegisterPosDeviceCommand command) {
    var cmd =
        new RegisterPosDeviceCommand(
            tenantId,
            command.outletId(),
            command.deviceId(),
            command.label(),
            command.capabilities());
    return commandBus.send(cmd);
  }

  @PostMapping("/{id}/heartbeat")
  public void sendHeartbeat(
      @PathVariable TenantId tenantId,
      @PathVariable TerminalId id,
      @RequestBody SendPosHeartbeatCommand command) {
    var cmd =
        new SendPosHeartbeatCommand(
            tenantId,
            id,
            command.lastSeenAt(),
            command.status(),
            command.batteryPercent(),
            command.appVersion(),
            command.extras());
    commandBus.send(cmd);
  }

  @PostMapping("/{id}/lock")
  public ApiResponse<TerminalResponse> lockDevice(
      @PathVariable TenantId tenantId,
      @PathVariable TerminalId id,
      @RequestBody LockRequest request) {
    var cmd = new LockTerminalCommand(tenantId, id, request.actorId(), request.reason());
    Terminal res = commandBus.send(cmd);
    if (res == null) return ApiResponse.success(null);
    var dto = TerminalResponse.fromDomain(res);
    return ApiResponse.success(dto);
  }

  @PostMapping("/{id}/unlock")
  public ApiResponse<TerminalResponse> unlockDevice(
      @PathVariable TenantId tenantId,
      @PathVariable TerminalId id,
      @RequestBody UnlockRequest request) {
    var cmd = new UnlockTerminalCommand(tenantId, id, request.actorId());
    Terminal res = commandBus.send(cmd);
    if (res == null) return ApiResponse.success(null);
    var dto = TerminalResponse.fromDomain(res);
    return ApiResponse.success(dto);
  }

  @PutMapping("/{id}/metadata")
  public ApiResponse<TerminalResponse> updateMetadata(
      @PathVariable TenantId tenantId,
      @PathVariable TerminalId id,
      @RequestBody UpdateMetadataRequest request) {
    var cmd =
        new UpdateTerminalMetadataCommand(
            tenantId, id, request.actorId(), request.metadataPatch(), request.heartbeatAlso());
    Terminal res = commandBus.send(cmd);
    if (res == null) return ApiResponse.success(null);
    var dto = TerminalResponse.fromDomain(res);
    return ApiResponse.success(dto);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<TerminalResponse> unregisterDevice(
      @PathVariable TenantId tenantId,
      @PathVariable TerminalId id,
      @RequestBody UnregisterRequest request) {
    var cmd = new UnregisterTerminalCommand(tenantId, id, request.actorId(), request.reason());
    Terminal res = commandBus.send(cmd);
    if (res == null) return ApiResponse.success(null);
    var dto = TerminalResponse.fromDomain(res);
    return ApiResponse.success(dto);
  }

  @GetMapping("/{id}")
  public ApiResponse<TerminalResponse> getDevice(
      @PathVariable TenantId tenantId, @PathVariable TerminalId id) {
    var query = new GetPosDeviceByIdQuery(tenantId, id);
    Optional<Terminal> opt = queryBus.send(query);
    if (opt.isEmpty()) return ApiResponse.success(null);
    var dto = TerminalResponse.fromDomain(opt.get());
    return ApiResponse.success(dto);
  }

  @GetMapping("/{id}/status")
  public Map<String, Object> getDeviceStatus(
      @PathVariable TenantId tenantId, @PathVariable TerminalId id) {
    var query = new GetPosDeviceStatusQuery(tenantId, id);
    return queryBus.send(query);
  }

  @GetMapping("/outlets/{outletId}")
  public ApiResponse<List<TerminalResponse>> listDevicesByOutlet(
      @PathVariable TenantId tenantId, @PathVariable OutletId outletId) {
    var query = new ListPosDevicesByLocationQuery(tenantId, outletId);
    var terminals = queryBus.send(query);
    var dtos = terminals.stream().map(TerminalResponse::fromDomain).toList();
    return ApiResponse.success(dtos);
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<List<TerminalResponse>> listDevicesByTenant(@PathVariable TenantId tenantId) {
    var query = new ListPosDevicesByTenantQuery(tenantId);
    var terminals = queryBus.send(query);
    var dtos = terminals.stream().map(TerminalResponse::fromDomain).toList();
    return ApiResponse.success(dtos);
  }

  // Request DTOs
  public record LockRequest(UUID actorId, String reason) {}

  public record UnlockRequest(UUID actorId) {}

  public record UpdateMetadataRequest(
      UUID actorId, Map<String, Object> metadataPatch, boolean heartbeatAlso) {}

  public record UnregisterRequest(UUID actorId, String reason) {}
}
