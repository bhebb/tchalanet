package com.tchalanet.server.core.terminal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.terminal.application.command.model.ActivateTerminalForUserCommand;
import com.tchalanet.server.core.terminal.application.command.model.AssignTerminalToOutletCommand;
import com.tchalanet.server.core.terminal.application.command.model.AssignTerminalToUserCommand;
import com.tchalanet.server.core.terminal.application.command.model.LockTerminalCommand;
import com.tchalanet.server.core.terminal.application.command.model.RegisterTerminalCommand;
import com.tchalanet.server.core.terminal.application.command.model.UnlockTerminalCommand;
import com.tchalanet.server.core.terminal.application.command.model.UnregisterTerminalCommand;
import com.tchalanet.server.core.terminal.application.command.model.UpdateTerminalMetadataCommand;
import com.tchalanet.server.core.terminal.application.command.model.UpdateTerminalSyncStateCommand;
import com.tchalanet.server.core.terminal.application.query.model.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.application.query.model.ListOfflineTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.ListSyncPendingTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSummaryView;
import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TerminalAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  // ── DTOs ────────────────────────────────────────────────────────────────

  public record RegisterTerminalRequest(
      @NotNull OutletId outletId,
      TerminalKind kind,
      @NotBlank String label,
      String inventoryTag,
      Map<String, Object> metadata) {}

  public record UnregisterTerminalRequest(@NotBlank String reason) {}

  public record LockTerminalRequest(@NotBlank String reason) {}

  public record AssignOutletRequest(@NotNull OutletId outletId) {}

  public record AssignUserRequest(@NotNull UserId userId) {}

  public record UpdateMetadataRequest(@NotNull Map<String, Object> metadataPatch) {}

  public record UpdateSyncStateRequest(@NotNull TerminalSyncState newSyncState) {}

  // ── List & detail ──────────────────────────────────────────────────────

  @GetMapping
  public ApiResponse<TchPage<TerminalSummaryView>> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) OutletId outletId,
      @RequestParam(required = false) UserId assignedUserId,
      @RequestParam(required = false) TerminalKind kind,
      @RequestParam(required = false) TerminalState state,
      @RequestParam(required = false) TerminalSyncState syncState,
      @RequestParam(required = false) Boolean activeForUser,
      @TchPaging(defaultSort = {"label,ASC"}, allowedSort = {"label", "createdAt", "lastSeen"})
          TchPageRequest pageRequest) {
    var criteria =
        new TerminalSearchCriteria(q, outletId, assignedUserId, kind, state, syncState, activeForUser);
    return ApiResponse.success(queryBus.ask(new ListTerminalsQuery(criteria, pageRequest)));
  }

  @GetMapping("/{id}")
  public ApiResponse<TerminalView> get(@PathVariable TerminalId id) {
    return ApiResponse.success(queryBus.ask(new GetTerminalByIdQuery(id)));
  }

  @GetMapping("/offline")
  public ApiResponse<List<TerminalSummaryView>> listOffline() {
    return ApiResponse.success(queryBus.ask(new ListOfflineTerminalsQuery()));
  }

  @GetMapping("/sync-pending")
  public ApiResponse<List<TerminalSummaryView>> listSyncPending() {
    return ApiResponse.success(queryBus.ask(new ListSyncPendingTerminalsQuery()));
  }

  // ── Register / Unregister ──────────────────────────────────────────────

  @PostMapping
  @AuditLog(
      entity = AuditEntityType.TERMINAL,
      action = AuditAction.TERMINAL_REGISTER,
      idExpression = "#result.data.value().toString()",
      detailsExpression = "#req")
  public ApiResponse<TerminalId> register(
      @CurrentContext TchRequestContext ctx, @Valid @RequestBody RegisterTerminalRequest req) {
    return ApiResponse.success(
        commandBus.execute(
            new RegisterTerminalCommand(
                ctx.tenantIdSafe(),
                req.outletId(),
                req.kind() == null ? TerminalKind.PHYSICAL : req.kind(),
                req.label(),
                req.inventoryTag(),
                req.metadata(),
                ctx.currentUserIdRequired())));
  }

  @DeleteMapping("/{id}")
  @AuditLog(
      entity = AuditEntityType.TERMINAL,
      action = AuditAction.TERMINAL_UNREGISTER,
      idExpression = "#id.value().toString()",
      detailsExpression = "#req")
  public ApiResponse<Void> unregister(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TerminalId id,
      @Valid @RequestBody UnregisterTerminalRequest req) {
    commandBus.execute(
        new UnregisterTerminalCommand(
            ctx.tenantIdSafe(), id, req.reason(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  // ── Lock / Unlock ──────────────────────────────────────────────────────

  @PostMapping("/{id}/lock")
  @AuditLog(
      entity = AuditEntityType.TERMINAL,
      action = AuditAction.TERMINAL_LOCK,
      idExpression = "#id.value().toString()",
      detailsExpression = "#req")
  public ApiResponse<Void> lock(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TerminalId id,
      @Valid @RequestBody LockTerminalRequest req) {
    commandBus.execute(
        new LockTerminalCommand(ctx.tenantIdSafe(), id, req.reason(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @PostMapping("/{id}/unlock")
  @AuditLog(
      entity = AuditEntityType.TERMINAL,
      action = AuditAction.TERMINAL_UNLOCK,
      idExpression = "#id.value().toString()")
  public ApiResponse<Void> unlock(
      @CurrentContext TchRequestContext ctx, @PathVariable TerminalId id) {
    commandBus.execute(
        new UnlockTerminalCommand(ctx.tenantIdSafe(), id, ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  // ── Assignment ──────────────────────────────────────────────────────────

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

  // ── Metadata / Sync ─────────────────────────────────────────────────────

  @PatchMapping("/{id}/metadata")
  @AuditLog(
      entity = AuditEntityType.TERMINAL,
      action = AuditAction.TERMINAL_METADATA_UPDATE,
      idExpression = "#id.value().toString()")
  public ApiResponse<Void> updateMetadata(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TerminalId id,
      @Valid @RequestBody UpdateMetadataRequest req) {
    commandBus.execute(
        new UpdateTerminalMetadataCommand(
            ctx.tenantIdSafe(), id, req.metadataPatch(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @PostMapping("/{id}/sync-state")
  @AuditLog(
      entity = AuditEntityType.TERMINAL,
      action = AuditAction.TERMINAL_SYNC_STATE_UPDATE,
      idExpression = "#id.value().toString()",
      detailsExpression = "#req")
  public ApiResponse<Void> updateSyncState(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TerminalId id,
      @Valid @RequestBody UpdateSyncStateRequest req) {
    commandBus.execute(
        new UpdateTerminalSyncStateCommand(
            ctx.tenantIdSafe(), id, req.newSyncState(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }
}
