package com.tchalanet.server.platform.tenantgame.internal.web;

import com.tchalanet.server.catalog.plan.api.PlanFeatureKeys;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameAdminView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameCatalogItemView;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameAdminService;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameCatalogProjectionService;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Admin • Games")
@RestController
@RequestMapping("/admin/games")
@PreAuthorize("hasPermission(null, 'game-pricing.read')")
@RequiredArgsConstructor
public class TenantGameAdminController {

    private final TenantGameAdminService adminService;
    private final TenantGameCatalogProjectionService catalogProjection;
    private final GameCatalog gameCatalog;

    @Operation(summary = "List tenant games")
    @GetMapping
    public ApiResponse<List<TenantGameAdminView>> list(@CurrentContext TchRequestContext ctx) {
        var tenantId = ctx.effectiveTenantIdRequired();
        var games = adminService.listGames(tenantId);
        var views = games.stream().map(g -> {
            var catalogName = gameCatalog.findByCode(g.gameCode()).map(gv -> gv.name()).orElse(g.gameCode());
            var category = gameCatalog.findByCode(g.gameCode()).map(gv -> gv.category()).orElse(null);
            return new TenantGameAdminView(
                g.gameCode(), catalogName, category, g.displayName(),
                g.enabled(), g.visibleInPos(), g.displayOrder(),
                g.minStake(), g.maxStake(),
                g.availabilityEnabled(), g.availabilityDays(),
                g.startLocalTime() != null ? g.startLocalTime().toString() : null,
                g.endLocalTime() != null ? g.endLocalTime().toString() : null,
                g.enabled());
        }).toList();
        return ApiResponse.success(views);
    }

    @Operation(summary = "Catalog projection — all catalog games with tenant status")
    @GetMapping("/catalog")
    public ApiResponse<List<TenantGameCatalogItemView>> catalog(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(catalogProjection.getCatalogProjection(ctx.effectiveTenantIdRequired()));
    }

    @Operation(summary = "Enable a catalog game for this tenant")
    @PostMapping("/{gameCode}/enable")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'game-pricing.update')")
    @RequiredFeature(PlanFeatureKeys.TENANTGAME_MANAGEMENT)
    public ApiResponse<EnableTenantGameResult> enable(
        @PathVariable String gameCode,
        @CurrentContext TchRequestContext ctx) {
        var result = adminService.enableGame(EnableTenantGameRequest.builder()
            .tenantId(ctx.effectiveTenantIdRequired())
            .gameCode(gameCode)
            .build());
        return ApiResponse.created(result);
    }

    @Operation(summary = "Disable a game for this tenant")
    @PostMapping("/{gameCode}/disable")
    @PreAuthorize("hasPermission(null, 'game-pricing.update')")
    public ApiResponse<DisableTenantGameResult> disable(
        @PathVariable String gameCode,
        @CurrentContext TchRequestContext ctx) {
        var result = adminService.disableGame(DisableTenantGameRequest.builder()
            .tenantId(ctx.effectiveTenantIdRequired())
            .gameCode(gameCode)
            .build());
        return ApiResponse.success(result);
    }

    @Operation(summary = "Update tenant game settings")
    @PatchMapping("/{gameCode}/settings")
    @PreAuthorize("hasPermission(null, 'game-pricing.update')")
    public ApiResponse<Void> updateSettings(
        @PathVariable String gameCode,
        @Valid @RequestBody UpdateGameSettingsWebRequest body,
        @CurrentContext TchRequestContext ctx) {
        adminService.updateSettings(UpdateTenantGameSettingsRequest.builder()
            .tenantId(ctx.effectiveTenantIdRequired())
            .gameCode(gameCode)
            .displayName(body.displayName())
            .displayOrder(body.displayOrder())
            .visibleInPos(body.visibleInPos())
            .minStake(body.minStake())
            .maxStake(body.maxStake())
            .availabilityEnabled(body.availabilityEnabled())
            .availabilityDays(body.availabilityDays())
            .startLocalTime(body.startLocalTime())
            .endLocalTime(body.endLocalTime())
            .build());
        return ApiResponse.success(null);
    }

    public record UpdateGameSettingsWebRequest(
        String displayName,
        Integer displayOrder,
        Boolean visibleInPos,
        BigDecimal minStake,
        BigDecimal maxStake,
        Boolean availabilityEnabled,
        String availabilityDays,
        String startLocalTime,
        String endLocalTime
    ) {}
}
