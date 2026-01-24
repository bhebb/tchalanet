package com.tchalanet.server.core.tenantgame.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.core.tenantgame.application.command.model.DisableTenantGameCommand;
import com.tchalanet.server.core.tenantgame.application.command.model.EnableTenantGameCommand;
import com.tchalanet.server.core.tenantgame.application.command.model.UpdateTenantGamePolicyCommand;
import com.tchalanet.server.core.tenantgame.application.command.model.DisableTenantGameCommandResult;
import com.tchalanet.server.core.tenantgame.application.command.model.EnableTenantGameCommandResult;
import com.tchalanet.server.core.tenantgame.application.query.model.ResolveTenantGamesQuery;
import com.tchalanet.server.core.tenantgame.infra.web.mapper.TenantGameWebMapper;
import com.tchalanet.server.core.tenantgame.infra.web.model.TenantGameView;
import com.tchalanet.server.core.tenantgame.infra.web.model.UpdatePolicyRequest;
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

import java.util.List;

/**
 * REST controller for tenant game management (core/tenantgame).
 * Follows all conventions:
 * - command_query_handlers.md: CommandBus/QueryBus dispatch
 * - typed_ids.md: TenantId typed wrapper
 * - web_api.md: ApiResponse<T> wrapping, @ResponseStatus
 * - request_context_usage.md: TchContext for tenant resolution (not path param)
 * - security_permissions.md: @PreAuthorize with permission checks
 */
@RestController
@RequestMapping("/tenant/games")
@RequiredArgsConstructor
public class TenantGameAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final TenantGameWebMapper webMapper;

  /**
   * List all enabled games for the current tenant.
   * Tenant resolved from TchContext (request context filter).
   * Per request_context_usage.md: tenant is always available in context.
   */
  @GetMapping
  @PreAuthorize("hasPermission('tenantgame.read')")
  public ApiResponse<List<TenantGameView>> getTenantGames() {
    var tenantId = TchContext.get().tenantId();
    var query = ResolveTenantGamesQuery.builder()
        .tenantId(tenantId)
        .build();
    var games = queryBus.send(query);
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
  @PreAuthorize("hasPermission('tenantgame.write')")
  public ApiResponse<EnableTenantGameCommandResult> enableGame(
      @PathVariable String gameCode) {
    var tenantId = TchContext.get().tenantId();
    var command = EnableTenantGameCommand.builder()
        .tenantId(tenantId)
        .gameCode(gameCode)
        .policy(null)
        .build();
    var result = commandBus.send(command);
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
  @PreAuthorize("hasPermission('tenantgame.write')")
  public ApiResponse<DisableTenantGameCommandResult> disableGame(
      @PathVariable String gameCode) {
    var tenantId = TchContext.get().tenantId();
    var command = DisableTenantGameCommand.builder()
        .tenantId(tenantId)
        .gameCode(gameCode)
        .build();
    var result = commandBus.send(command);
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
  @PreAuthorize("hasPermission('tenantgame.write')")
  public ApiResponse<Void> updatePolicy(
      @PathVariable String gameCode,
      @RequestBody UpdatePolicyRequest request) {
    var tenantId = TchContext.get().tenantId();
    var command = UpdateTenantGamePolicyCommand.builder()
        .tenantId(tenantId)
        .gameCode(gameCode)
        .policy(request.getPolicy())
        .build();
    commandBus.send(command);
    ApiResponseContext.get().addNotice(
        "POLICY_UPDATED",
        "Politique du jeu mise à jour",
        "tenantgame",
        NoticeSeverity.INFO
    );
    return ApiResponse.success(null);
  }
}
