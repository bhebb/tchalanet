package com.tchalanet.server.core.outlet.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.outlet.application.command.model.AssignUserToOutletCommand;
import com.tchalanet.server.core.outlet.application.command.model.BlockOutletSalesCommand;
import com.tchalanet.server.core.outlet.application.command.model.CreateOutletCommand;
import com.tchalanet.server.core.outlet.application.command.model.OutletConfigPatch;
import com.tchalanet.server.core.outlet.application.command.model.RemoveUserFromOutletCommand;
import com.tchalanet.server.core.outlet.application.command.model.UnblockOutletSalesCommand;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletOperationalContextQuery;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletSalesCapabilityQuery;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletTerminalsQuery;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletUsersQuery;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletsQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletOperationalContextView;
import com.tchalanet.server.core.outlet.application.query.model.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import com.tchalanet.server.core.outlet.application.query.model.OutletTerminalView;
import com.tchalanet.server.core.outlet.application.query.model.OutletUserView;
import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import com.tchalanet.server.core.outlet.domain.model.SalesCapability;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/admin/outlets")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class OutletAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  // ── DTOs ────────────────────────────────────────────────────────────────

  public record CreateOutletRequest(String name, String slug, AddressInput address) {}

  public record BlockSalesRequest(String reason) {}

  public record AssignUserRequest(UserId userId) {}

  // ── Listing & detail ────────────────────────────────────────────────────

  @GetMapping
  public ApiResponse<TchPage<OutletSummaryView>> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Boolean salesBlocked,
      @RequestParam(required = false) Boolean dayClosed,
      @TchPaging(defaultSort = {"name,ASC"}, allowedSort = {"name", "slug", "createdAt"})
          TchPageRequest pageRequest) {
    OutletSearchCriteria criteria = new OutletSearchCriteria(q, active, salesBlocked, dayClosed);
    return ApiResponse.success(queryBus.ask(new ListOutletsQuery(criteria, pageRequest)));
  }

  @GetMapping("/{id}")
  public ApiResponse<OutletView> get(
      @CurrentContext TchRequestContext ctx, @PathVariable OutletId id) {
    return ApiResponse.success(queryBus.ask(new GetOutletByIdQuery(ctx.tenantIdSafe(), id)));
  }

  @GetMapping("/{id}/operational-context")
  public ApiResponse<OutletOperationalContextView> operationalContext(@PathVariable OutletId id) {
    return ApiResponse.success(queryBus.ask(new GetOutletOperationalContextQuery(id)));
  }

  @GetMapping("/{id}/sales-capability")
  public ApiResponse<SalesCapability> salesCapability(@PathVariable OutletId id) {
    return ApiResponse.success(queryBus.ask(new GetOutletSalesCapabilityQuery(id)));
  }

  // ── CRUD ─────────────────────────────────────────────────────────────────

  @PostMapping
  @AuditLog(
      entity = AuditEntityType.OUTLET,
      action = AuditAction.OUTLET_CREATE,
      idExpression = "#result.data.toString()",
      detailsExpression = "#req")
  public ApiResponse<OutletId> create(
      @CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateOutletRequest req) {
    return ApiResponse.success(
        commandBus.execute(
            new CreateOutletCommand(ctx.tenantIdSafe(), req.name(), req.slug(), null, req.address())));
  }

  @PatchMapping("/{id}/config")
  @AuditLog(
      entity = AuditEntityType.OUTLET,
      action = AuditAction.OUTLET_UPDATE,
      idExpression = "#id.value().toString()",
      detailsExpression = "#patch")
  public ApiResponse<Void> updateConfig(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @RequestBody OutletConfigPatch patch) {
    commandBus.execute(new UpdateOutletConfigCommand(ctx.tenantIdSafe(), id, patch));
    return ApiResponse.success(null);
  }

  // ── Sales blocking ───────────────────────────────────────────────────────

  @PostMapping("/{id}/block-sales")
  @AuditLog(
      entity = AuditEntityType.OUTLET,
      action = AuditAction.OUTLET_BLOCK_SALES,
      idExpression = "#id.value().toString()",
      detailsExpression = "#req")
  public ApiResponse<Void> blockSales(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @Valid @RequestBody BlockSalesRequest req) {
    commandBus.execute(
        new BlockOutletSalesCommand(
            ctx.tenantIdSafe(), id, req.reason(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @PostMapping("/{id}/unblock-sales")
  @AuditLog(
      entity = AuditEntityType.OUTLET,
      action = AuditAction.OUTLET_UNBLOCK_SALES,
      idExpression = "#id.value().toString()")
  public ApiResponse<Void> unblockSales(
      @CurrentContext TchRequestContext ctx, @PathVariable OutletId id) {
    commandBus.execute(
        new UnblockOutletSalesCommand(ctx.tenantIdSafe(), id, ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  // ── User assignment ──────────────────────────────────────────────────────

  @GetMapping("/{id}/users")
  public ApiResponse<List<OutletUserView>> listUsers(@PathVariable OutletId id) {
    return ApiResponse.success(queryBus.ask(new ListOutletUsersQuery(id)));
  }

  @PostMapping("/{id}/users")
  @AuditLog(
      entity = AuditEntityType.OUTLET,
      action = AuditAction.OUTLET_USER_ASSIGN,
      idExpression = "#id.value().toString()",
      detailsExpression = "#req")
  public ApiResponse<Void> assignUser(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @Valid @RequestBody AssignUserRequest req) {
    commandBus.execute(
        new AssignUserToOutletCommand(
            ctx.tenantIdSafe(), id, req.userId(), ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @DeleteMapping("/{id}/users/{userId}")
  @AuditLog(
      entity = AuditEntityType.OUTLET,
      action = AuditAction.OUTLET_USER_REMOVE,
      idExpression = "#id.value().toString()",
      detailsExpression = "#userId")
  public ApiResponse<Void> removeUser(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @PathVariable UserId userId) {
    commandBus.execute(
        new RemoveUserFromOutletCommand(
            ctx.tenantIdSafe(), id, userId, ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  // ── Terminals ────────────────────────────────────────────────────────────

  @GetMapping("/{id}/terminals")
  public ApiResponse<List<OutletTerminalView>> listTerminals(@PathVariable OutletId id) {
    return ApiResponse.success(queryBus.ask(new ListOutletTerminalsQuery(id)));
  }
}
