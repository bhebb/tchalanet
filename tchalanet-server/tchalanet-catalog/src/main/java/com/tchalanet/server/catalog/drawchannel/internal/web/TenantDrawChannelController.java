package com.tchalanet.server.catalog.drawchannel.internal.web;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelActiveRequest;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelRequest;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateTenantDrawChannelRequest;
import com.tchalanet.server.catalog.drawchannel.internal.write.DrawChannelAdminService;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/draw-channels")
@RequiredArgsConstructor
@Tag(name = "Tenant • Draw Channels")
@PreAuthorize("hasAuthority('TENANT_READ') or hasPermission(null, 'draw_channel.read')")
public class TenantDrawChannelController {

  private final DrawChannelCatalog catalog;
  private final DrawChannelAdminService adminService;

  @Operation(summary = "List draw channels (tenant)")
  @GetMapping
  public ApiResponse<List<DrawChannelSummaryView>> list(
      @CurrentContext TchRequestContext ctx, @RequestParam(required = false) Boolean activeOnly) {
    var tenantId = ctx.tenantId();
    var list = catalog.listAll(tenantId, activeOnly);
    return ApiResponse.success(list);
  }

  @Operation(summary = "Get draw channel by id for current tenant")
  @GetMapping("/{id}")
  public ApiResponse<DrawChannelView> getById(
      @PathVariable DrawChannelId id, @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.tenantId();
    var opt = catalog.findById(tenantId, id);
    return ApiResponse.success(opt.orElse(null));
  }

  @Operation(summary = "Enable or disable a tenant draw channel")
  @PatchMapping("/{id}/active")
  @PreAuthorize("hasPermission(null, 'draw_channel.manage')")
  public ApiResponse<DrawChannelView> setActive(
      @PathVariable DrawChannelId id, @Valid @RequestBody UpdateDrawChannelActiveRequest req) {
    var view = adminService.setChannelActive(id, Boolean.TRUE.equals(req.active()));
    return ApiResponse.success(view);
  }

  @Operation(summary = "Update a tenant draw channel configuration")
  @PutMapping("/{id}")
  @PreAuthorize("hasPermission(null, 'draw_channel.manage')")
  public ApiResponse<DrawChannelView> update(
      @PathVariable DrawChannelId id,
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpdateTenantDrawChannelRequest req) {
    var existing =
        catalog
            .findById(ctx.tenantId(), id)
            .orElseThrow(() -> new IllegalArgumentException("draw_channel_not_found: " + id));
    var update =
        new UpdateDrawChannelRequest(
            existing.tenantId(),
            existing.code(),
            req.name() == null ? existing.name() : req.name(),
            req.label() == null ? existing.label() : req.label(),
            req.timezone() == null ? existing.timezone() : req.timezone(),
            req.drawTime() == null ? existing.drawTime() : req.drawTime(),
            req.cutoffSec() == null ? existing.cutoffSec() : req.cutoffSec(),
            req.daysOfWeek() == null ? existing.daysOfWeek() : req.daysOfWeek(),
            req.active() == null ? existing.active() : req.active(),
            req.sortOrder() == null ? existing.sortOrder() : req.sortOrder(),
            req.period() == null ? existing.period() : req.period(),
            req.notes() == null ? existing.notes() : req.notes(),
            existing.resultSlotId(),
            existing.defaultSource());
    var view = adminService.updateFromRequest(id, update);
    return ApiResponse.success(view);
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
