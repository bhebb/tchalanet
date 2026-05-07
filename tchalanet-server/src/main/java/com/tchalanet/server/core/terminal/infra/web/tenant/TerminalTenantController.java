package com.tchalanet.server.core.terminal.infra.web.tenant;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.application.command.model.SendTerminalHeartbeatCommand;
import com.tchalanet.server.core.terminal.application.command.model.UpdateTerminalSyncStateCommand;
import com.tchalanet.server.core.terminal.application.query.model.GetCurrentTerminalQuery;
import com.tchalanet.server.core.terminal.application.query.model.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  // ── DTOs ────────────────────────────────────────────────────────────────

  public record HeartbeatRequest(Instant occurredAt) {}

  public record SyncStateRequest(@NotNull TerminalSyncState newSyncState) {}

  // ── Read ────────────────────────────────────────────────────────────────

  @GetMapping("/current")
  public ApiResponse<TerminalView> current(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(queryBus.ask(new GetCurrentTerminalQuery(ctx.currentUserIdRequired())));
  }

  @GetMapping("/{id}/status")
  public ApiResponse<TerminalView> status(@PathVariable TerminalId id) {
    return ApiResponse.success(queryBus.ask(new GetTerminalByIdQuery(id)));
  }

  // ── Runtime ─────────────────────────────────────────────────────────────

  @PostMapping("/{id}/heartbeat")
  public ApiResponse<Void> heartbeat(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TerminalId id,
      @RequestBody(required = false) HeartbeatRequest req) {
    Instant occurredAt =
        (req == null || req.occurredAt() == null) ? Instant.now(clock) : req.occurredAt();
    commandBus.execute(new SendTerminalHeartbeatCommand(ctx.tenantIdSafe(), id, occurredAt));
    return ApiResponse.success(null);
  }

  @PostMapping("/{id}/sync-state")
  public ApiResponse<Void> reportSyncState(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TerminalId id,
      @Valid @RequestBody SyncStateRequest req) {
    commandBus.execute(
        new UpdateTerminalSyncStateCommand(
            ctx.tenantIdSafe(), id, req.newSyncState(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }
}
