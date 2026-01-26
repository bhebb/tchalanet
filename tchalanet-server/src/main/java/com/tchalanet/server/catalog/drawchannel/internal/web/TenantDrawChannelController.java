package com.tchalanet.server.catalog.drawchannel.internal.web;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenant/draw-channels")
@RequiredArgsConstructor
@Tag(name = "Tenant • Draw Channels")
@PreAuthorize("hasAuthority('TENANT_READ')")
public class TenantDrawChannelController {

    private final DrawChannelCatalog catalog;

    @Operation(summary = "List draw channels (tenant)")
    @GetMapping
    public ApiResponse<List<DrawChannelSummaryView>> list(
        @CurrentContext TchRequestContext ctx, @RequestParam(required = false) Boolean activeOnly) {
        var tenantId = ctx.tenantId();
        var list = catalog.listAll(tenantId, activeOnly);
        return ApiResponse.success(list);
    }

    @Operation(summary = "Get draw channel by code for current tenant (debug)")
    @GetMapping("/by-code/{code}")
    public ApiResponse<DrawChannelView> byCode(
        @PathVariable String code, @CurrentContext TchRequestContext ctx) {
        var tenantId = ctx.tenantId();
        var opt = catalog.findByTenantAndCode(tenantId, code);
        return ApiResponse.success(opt.orElse(null));
    }

    @Operation(summary = "List games grouped by channel for current tenant")
    @GetMapping("/games")
    public ApiResponse<List<ChannelGamesView>> gamesMap(@CurrentContext TchRequestContext ctx) {
        var tenantId = ctx.tenantId();
        var list = catalog.listChannelGames(tenantId);
        return ApiResponse.success(list);
    }
}
