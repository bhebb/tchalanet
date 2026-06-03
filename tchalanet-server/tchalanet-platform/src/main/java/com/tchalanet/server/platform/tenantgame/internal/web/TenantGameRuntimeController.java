package com.tchalanet.server.platform.tenantgame.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRuntimeView;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tenant Runtime")
@RestController
@RequestMapping("/tenant/games")
@RequiredArgsConstructor
public class TenantGameRuntimeController {

    private final TenantGameRuntimeService runtimeService;

    @Operation(summary = "Runtime games for POS/sales/bootstrap — enabled games only, safe view")
    @GetMapping("/runtime")
    public ApiResponse<List<TenantGameRuntimeView>> runtime(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(runtimeService.getRuntimeGames(ctx.effectiveTenantIdRequired()));
    }
}
