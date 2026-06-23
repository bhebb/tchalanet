package com.tchalanet.server.features.tenantadmin.game;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.DrawChannelGameResponse;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelGameRequest;
import com.tchalanet.server.catalog.drawchannel.internal.write.DrawChannelGameAdminService;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;

@Tag(name = "Admin • Draw Channel Games")
@RestController
@RequestMapping("/admin/draw-channels/{drawChannelId}/tenant-games")
@PreAuthorize("hasPermission('tenantgame.manage')")
@RequiredArgsConstructor
public class TenantAdminDrawChannelGameController {

    private final DrawChannelCatalog drawChannelCatalog;
    private final TenantGameApi tenantGameApi;
    private final GameCatalog gameCatalog;
    private final DrawChannelGameAdminService admin;

    @Operation(summary = "Offer a tenant game on a draw channel (upsert)")
    @PutMapping("/{tenantGameId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<DrawChannelGameResponse> offer(
        @PathVariable DrawChannelId drawChannelId,
        @PathVariable TenantGameId tenantGameId,
        @RequestBody OfferChannelGameRequest body,
        @CurrentContext TchRequestContext ctx) {

        var tenantId = ctx.tenantId();

        drawChannelCatalog.findById(tenantId, drawChannelId)
            .orElseThrow(() -> ProblemRest.notFound("draw_channel.not_found", drawChannelId));

        var tenantGame = tenantGameApi.findByTenantGameId(tenantId, tenantGameId)
            .orElseThrow(() -> ProblemRest.notFound("tenant_game.not_found", tenantGameId));

        if (!tenantGame.enabled()) {
            throw ProblemRest.conflict("tenant_game.disabled");
        }

        gameCatalog.findById(tenantGame.gameId())
            .filter(g -> g.active())
            .orElseThrow(() -> ProblemRest.conflict("catalog_game.inactive"));

        var enabled = body.enabled() != null ? body.enabled() : true;
        var result = admin.upsert(tenantId, drawChannelId, tenantGameId, enabled, body.flags());
        return ApiResponse.success(result);
    }

    @Operation(summary = "Enable or disable a tenant game on a draw channel")
    @PatchMapping("/{tenantGameId}")
    public ApiResponse<DrawChannelGameResponse> patch(
        @PathVariable DrawChannelId drawChannelId,
        @PathVariable TenantGameId tenantGameId,
        @RequestBody UpdateDrawChannelGameRequest body,
        @CurrentContext TchRequestContext ctx) {

        var tenantId = ctx.tenantId();

        drawChannelCatalog.findById(tenantId, drawChannelId)
            .orElseThrow(() -> ProblemRest.notFound("draw_channel.not_found", drawChannelId));

        var result = admin.update(tenantId, drawChannelId, tenantGameId, body);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Remove a tenant game from a draw channel")
    @DeleteMapping("/{tenantGameId}")
    public ApiResponse<Void> remove(
        @PathVariable DrawChannelId drawChannelId,
        @PathVariable TenantGameId tenantGameId,
        @CurrentContext TchRequestContext ctx) {

        var tenantId = ctx.tenantId();

        drawChannelCatalog.findById(tenantId, drawChannelId)
            .orElseThrow(() -> ProblemRest.notFound("draw_channel.not_found", drawChannelId));

        admin.softDelete(tenantId, drawChannelId, tenantGameId);
        return ApiResponse.success(null);
    }

    public record OfferChannelGameRequest(Boolean enabled, JsonNode flags) {}
}
