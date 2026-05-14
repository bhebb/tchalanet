package com.tchalanet.server.platform.tenantgame.internal.web;

import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.ResolveTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGamePolicyRequest;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameService;
import com.tchalanet.server.platform.tenantgame.internal.web.mapper.TenantGameWebMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant game management (core/tenantgame).
 * Follows all conventions:
 * - typed_ids.md: TenantId typed wrapper
 * - web_api.md: ApiResponse<T> wrapping, @ResponseStatus
 * - request_context_usage.md: TchContext for tenant resolution (not path param)
 * - security_permissions.md: @PreAuthorize with permission checks
 */
@RestController
@RequestMapping("/tenant/games")
@RequiredArgsConstructor
public class TenantGameAdminController {

  private final TenantGameService tenantGameService;
  private final TenantGameWebMapper webMapper;

  /**
   * List all enabled games for the current tenant.
   * Tenant resolved from TchContext (request context filter).
   * Per request_context_usage.md: tenant is always available in context.
   */
  @GetMapping
  @PreAuthorize("hasPermission('tenantgame.read')")
  public ApiResponse<List<TenantGameView>> getTenantGames(@CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.effectiveTenantIdRequired();
    var query = ResolveTenantGamesRequest.builder()
        .tenantId(tenantId)
        .build();
    var games = tenantGameService.resolveTenantGames(query);
    var views = games.stream().map(webMapper::toView).toList();
    return ApiResponse.success(views);
  }

  /**
   * Enable a game for the current tenant.
   * Permission check: tenantgame.write (per security_permissions.md).
   * Tenant resolved from context, not from client input.
   */
  @PostMapping("/{gameCode}/enable")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasPermission(null, 'tenantgame.write')")
  public ApiResponse<EnableTenantGameResult> enableGame(
      @PathVariable String gameCode,
      @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.effectiveTenantIdRequired();
    var command = EnableTenantGameRequest.builder()
        .tenantId(tenantId)
        .gameCode(gameCode)
        .policy(null)
        .build();
    var result = tenantGameService.enableTenantGame(command);
    ApiResponseContext.get().addNotice(
        "GAME_ENABLED",
        "Jeu activé pour le tenant",
        "tenantgame",
        NoticeSeverity.INFO
    );
    return ApiResponse.created(result);
  }

  /**
   * Disable a game for the current tenant.
   * Permission check: tenantgame.write (per security_permissions.md).
   */
  @PostMapping("/{gameCode}/disable")
  @PreAuthorize("hasPermission(null, 'tenantgame.write')")
  public ApiResponse<DisableTenantGameResult> disableGame(
      @PathVariable String gameCode,
      @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.effectiveTenantIdRequired();
    var command = DisableTenantGameRequest.builder()
        .tenantId(tenantId)
        .gameCode(gameCode)
        .build();
    var result = tenantGameService.disableTenantGame(command);
    ApiResponseContext.get().addNotice(
        "GAME_DISABLED",
        "Jeu désactivé pour le tenant",
        "tenantgame",
        NoticeSeverity.INFO
    );
    return ApiResponse.success(result);
  }

  /**
   * Update policy for a game in the current tenant.
   * Permission check: tenantgame.write (per security_permissions.md).
   */
  @PutMapping("/{gameCode}/policy")
  @PreAuthorize("hasPermission(null, 'tenantgame.write')")
  public ApiResponse<Void> updatePolicy(
      @PathVariable String gameCode,
      @RequestBody UpdatePolicyRequest request,
      @CurrentContext TchRequestContext ctx) {
    var tenantId = ctx.effectiveTenantIdRequired();
    var command = UpdateTenantGamePolicyRequest.builder()
        .tenantId(tenantId)
        .gameCode(gameCode)
        .policy(request.getPolicy())
        .build();
    tenantGameService.updateTenantGamePolicy(command);
    ApiResponseContext.get().addNotice(
        "POLICY_UPDATED",
        "Politique du jeu mise à jour",
        "tenantgame",
        NoticeSeverity.INFO
    );
    return ApiResponse.success(null);
  }
}
