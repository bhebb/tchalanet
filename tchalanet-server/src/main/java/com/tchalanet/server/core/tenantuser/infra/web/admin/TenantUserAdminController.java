package com.tchalanet.server.core.tenantuser.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantCommand;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantResult;
import com.tchalanet.server.core.tenantuser.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tenant-users")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantUserAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  public ApiResponse<AssignUserToTenantResult> assign(@RequestBody AssignUserToTenantCommand cmd) {
    var res = commandBus.send(cmd);
    return ApiResponse.success(res);
  }

  @GetMapping
  public ApiResponse<TchPage<TenantUserRow>> list(@CurrentContext TchRequestContext ctx, @TchPaging TchPageRequest pageReq) {
    var page = queryBus.send(new PagedListTenantUsersQuery(ctx.tenantId(), pageReq));
    return ApiResponse.success(TchPageMapper.map(page, this::toResponse));
  }

  private TenantUserRow toResponse(TenantUserRow r) {
    // minimal mapper (UI mapping will be expanded)
    return r;
  }
}
