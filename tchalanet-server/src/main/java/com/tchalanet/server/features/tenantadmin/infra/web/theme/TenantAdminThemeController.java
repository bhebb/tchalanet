package com.tchalanet.server.features.tenantadmin.infra.web.theme;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.tenantadmin.application.command.model.UpdateTenantThemeCommand;
import com.tchalanet.server.features.tenantadmin.application.query.model.GetTenantThemeQuery;
import com.tchalanet.server.features.tenantadmin.infra.web.model.TenantThemeRequest;
import com.tchalanet.server.features.tenantadmin.infra.web.model.TenantThemeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant-admin/theme")
@RequiredArgsConstructor
public class TenantAdminThemeController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @GetMapping
  public ApiResponse<TenantThemeResponse> getTheme(@CurrentContext TchRequestContext ctx) {
    var res = queryBus.send(new GetTenantThemeQuery(ctx.tenantId()));
    return ApiResponse.success(res);
  }

  @PutMapping
  public ApiResponse<Void> updateTheme(@CurrentContext TchRequestContext ctx, @RequestBody TenantThemeRequest req) {
    commandBus.send(new UpdateTenantThemeCommand(ctx.tenantId(), req.presetId(), req.overrides()));
    return ApiResponse.success(null);
  }
}
