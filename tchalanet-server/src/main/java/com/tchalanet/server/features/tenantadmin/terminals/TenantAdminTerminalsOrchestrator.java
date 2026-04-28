package com.tchalanet.server.features.tenantadmin.terminals;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.application.command.model.*;
import com.tchalanet.server.core.terminal.application.query.model.*;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAdminTerminalsOrchestrator {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public UUID registerDevice(TchRequestContext ctx, RegisterPosDeviceCommand cmd) {
    // build command ensuring tenant from context
    var tenantId = ctx.tenantIdSafe();
    var c = new RegisterPosDeviceCommand(tenantId, cmd.outletId(), cmd.deviceId(), cmd.label(), cmd.capabilities());
    return commandBus.send(c);
  }

  public void sendHeartbeat(TchRequestContext ctx, TerminalId id, SendPosHeartbeatCommand body) {
    var tenantId = ctx.tenantIdSafe();
    var c = new SendPosHeartbeatCommand(tenantId, id, body.lastSeenAt(), body.status(), body.batteryPercent(), body.appVersion(), body.extras());
    commandBus.send(c);
  }

  public Terminal lockDevice(TchRequestContext ctx, TerminalId id, UUID actorId, String reason) {
    var tenantId = ctx.tenantIdSafe();
    var cmd = new LockTerminalCommand(tenantId, id, actorId, reason);
    return commandBus.send(cmd);
  }

  public Terminal unlockDevice(TchRequestContext ctx, TerminalId id, UUID actorId) {
    var tenantId = ctx.tenantIdSafe();
    var cmd = new UnlockTerminalCommand(tenantId, id, actorId);
    return commandBus.send(cmd);
  }

  public Terminal updateMetadata(TchRequestContext ctx, TerminalId id, UUID actorId, Map<String, Object> metadataPatch, boolean heartbeatAlso) {
    var tenantId = ctx.tenantIdSafe();
    var cmd = new UpdateTerminalMetadataCommand(tenantId, id, actorId, metadataPatch, heartbeatAlso);
    return commandBus.send(cmd);
  }

  public Terminal unregisterDevice(TchRequestContext ctx, TerminalId id, UUID actorId, String reason) {
    var tenantId = ctx.tenantIdSafe();
    var cmd = new UnregisterTerminalCommand(tenantId, id, actorId, reason);
    return commandBus.send(cmd);
  }

  public Optional<Terminal> getDevice(TchRequestContext ctx, TerminalId id) {
    var tenantId = ctx.tenantIdSafe();
    var q = new GetPosDeviceByIdQuery(tenantId, id);
    return queryBus.send(q);
  }

  public Map<String, Object> getDeviceStatus(TchRequestContext ctx, TerminalId id) {
    var tenantId = ctx.tenantIdSafe();
    var q = new GetPosDeviceStatusQuery(tenantId, id);
    return queryBus.send(q);
  }

  public List<Terminal> listDevicesByOutlet(TchRequestContext ctx, OutletId outletId) {
    var tenantId = ctx.tenantIdSafe();
    var q = new ListPosDevicesByLocationQuery(tenantId, outletId);
    return queryBus.send(q);
  }

  public List<Terminal> listDevicesByTenant(TchRequestContext ctx) {
    var tenantId = ctx.tenantIdSafe();
    var q = new ListPosDevicesByTenantQuery(tenantId);
    return queryBus.send(q);
  }
}
