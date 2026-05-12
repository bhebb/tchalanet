package com.tchalanet.server.catalog.drawchannel.internal.web;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView;
import com.tchalanet.server.catalog.drawchannel.internal.write.DrawChannelGameAdminService;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.CreateDrawChannelGameRequest;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.DrawChannelGameResponse;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelGameRequest;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/draw-channels/{channelId}/games")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Platform • Draw Channel Games")
public class PlatformDrawChannelGameController {

  private final DrawChannelCatalog catalog; // read side
  private final DrawChannelGameAdminService admin; // write side

  @Operation(summary = "List games for a draw channel (platform)")
  @GetMapping
  public ApiResponse<List<DrawChannelGameView>> list(
      @PathVariable DrawChannelId channelId, @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.tenantId();
    var list = catalog.listGamesByChannel(tenantId, channelId);
    return ApiResponse.success(list);
  }

  @Operation(summary = "Attach or update a game to a channel (upsert)")
  @PostMapping
  public ApiResponse<DrawChannelGameResponse> upsert(
      @PathVariable DrawChannelId channelId,
      @RequestBody CreateDrawChannelGameRequest req,
      @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.tenantId();
    var res = admin.upsert(tenantId, channelId, req.gameId(), req.enabled(), req.flags());
    return ApiResponse.success(res);
  }

  @Operation(summary = "Bulk upsert games for a channel")
  @PostMapping("/bulk")
  public ApiResponse<List<DrawChannelGameResponse>> bulkUpsert(
      @PathVariable DrawChannelId channelId,
      @RequestBody List<CreateDrawChannelGameRequest> reqItems,
      @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.tenantId();
    var resp = admin.bulkUpsert(tenantId, channelId, reqItems);
    return ApiResponse.success(resp);
  }

  @Operation(summary = "Update an existing association")
  @PutMapping("/{gameId}")
  public ApiResponse<DrawChannelGameResponse> update(
      @PathVariable DrawChannelId channelId,
      @PathVariable GameId gameId,
      @RequestBody UpdateDrawChannelGameRequest req,
      @CurrentContext TchRequestContext ctx) {
    var res = admin.update(channelId, gameId, req);
    return ApiResponse.success(res);
  }

  @Operation(summary = "Soft-delete association")
  @DeleteMapping("/{gameId}")
  public ApiResponse<Void> delete(
      @PathVariable DrawChannelId channelId, @PathVariable GameId gameId) {
    admin.softDelete(channelId, gameId);
    return ApiResponse.success(null);
  }
}
