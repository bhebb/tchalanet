package com.tchalanet.server.features.tenantadmin.infra.web.user;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.tenantadmin.application.command.model.ProvisionTenantUserCommand;
import com.tchalanet.server.features.tenantadmin.application.command.model.ProvisionTenantUserResult;
import com.tchalanet.server.features.tenantadmin.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserDetails;
import com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserRow;
import com.tchalanet.server.features.tenantadmin.infra.web.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant-admin/users")
@RequiredArgsConstructor
public class TenantAdminUsersController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  public ApiResponse<ProvisionTenantUserResult> provision(@CurrentContext TchRequestContext ctx, @RequestBody ProvisionTenantUserRequest req) {
    // Build feature-level command and dispatch
    var cmd = new ProvisionTenantUserCommand(ctx.tenantId(), req.email(), req.firstName(), req.lastName(), req.roleId(), req.preferences());
    var res = commandBus.send(cmd);
    return ApiResponse.success(res);
  }

  @GetMapping
  public ApiResponse<TchPage<TenantUserRow>> list(@CurrentContext TchRequestContext ctx, @TchPaging TchPageRequest pageReq) {
    var page = queryBus.send(new PagedListTenantUsersQuery(ctx.tenantId(), pageReq));
    return ApiResponse.success(page);
  }

  @GetMapping("/{userId}")
  public ApiResponse<TenantUserDetails> get(@CurrentContext TchRequestContext ctx, @PathVariable java.util.UUID userId) {
    var details = queryBus.send(new com.tchalanet.server.features.tenantadmin.application.query.model.GetTenantUserDetailsQuery(ctx.tenantId(), com.tchalanet.server.common.types.id.UserId.of(userId)));
    return ApiResponse.success(details);
  }

  @PostMapping("/{userId}/suspend")
  public ApiResponse<Void> suspend(@CurrentContext TchRequestContext ctx, @PathVariable java.util.UUID userId, @RequestBody SuspendRequest req) {
    commandBus.send(new com.tchalanet.server.features.tenantadmin.application.command.model.SuspendTenantUserCommand(ctx.tenantId(), com.tchalanet.server.common.types.id.UserId.of(userId), req.reason()));
    return ApiResponse.success(null);
  }

  @PostMapping("/{userId}/reactivate")
  public ApiResponse<Void> reactivate(@CurrentContext TchRequestContext ctx, @PathVariable java.util.UUID userId) {
    commandBus.send(new com.tchalanet.server.features.tenantadmin.application.command.model.ReactivateTenantUserCommand(ctx.tenantId(), com.tchalanet.server.common.types.id.UserId.of(userId)));
    return ApiResponse.success(null);
  }

  @PostMapping("/{userId}/role")
  public ApiResponse<Void> changeRole(@CurrentContext TchRequestContext ctx, @PathVariable java.util.UUID userId, @RequestBody RoleChangeRequest req) {
    commandBus.send(new com.tchalanet.server.features.tenantadmin.application.command.model.AssignTenantUserRoleCommand(ctx.tenantId(), com.tchalanet.server.common.types.id.UserId.of(userId), req.roleId()));
    return ApiResponse.success(null);
  }

  @PostMapping("/{userId}/autonomy")
  public ApiResponse<Void> changeAutonomy(@CurrentContext TchRequestContext ctx, @PathVariable java.util.UUID userId, @RequestBody AutonomyChangeRequest req) {
    commandBus.send(new com.tchalanet.server.features.tenantadmin.application.command.model.ChangeTenantUserAutonomyCommand(ctx.tenantId(), com.tchalanet.server.common.types.id.UserId.of(userId), req.autonomyLevel()));
    return ApiResponse.success(null);
  }
}
