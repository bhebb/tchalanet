package com.tchalanet.server.catalog.game.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.catalog.game.api.TenantGameCatalog;
import com.tchalanet.server.catalog.game.application.command.model.EnsureTenantGamesCommand;
import com.tchalanet.server.catalog.game.application.command.model.UpdateTenantGameCommand;
import com.tchalanet.server.catalog.game.domain.model.TenantGame;
import com.tchalanet.server.catalog.game.infra.web.model.EnsureTenantGamesRequest;
import com.tchalanet.server.catalog.game.infra.web.model.EnsureTenantGamesResponse;
import com.tchalanet.server.catalog.game.infra.web.model.TenantGameUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tenant/games")
@RequiredArgsConstructor
@Tag(name = "Admin • Tenant Games")
public class TenantGameAdminController {
  private final TenantGameCatalog tenantGameCatalog;

  private final CommandBus commandBus;

  @Operation(summary = "List all tenant games (admin)")
  @GetMapping
  public ApiResponse<List<TenantGame>> listAll() {
    return ApiResponse.success(tenantGameCatalog.listAll());
  }

  @Operation(summary = "List enabled tenant games (admin)")
  @GetMapping("/enabled")
  public ApiResponse<List<TenantGame>> listEnabled() {
    return ApiResponse.success(tenantGameCatalog.listEnabled());
  }

  @Operation(summary = "Ensure tenant games exist (admin)")
  @PostMapping("/ensure")
  public ApiResponse<EnsureTenantGamesResponse> ensure(
      @RequestBody(required = false) EnsureTenantGamesRequest req) {
    var codes = req == null ? null : req.codes();
    var res = commandBus.send(new EnsureTenantGamesCommand(codes));
    var resp =
        new EnsureTenantGamesResponse(
            res.requestedCodes(), res.createdCodes(), res.alreadyAssignedCodes());
    return ApiResponse.success(resp);
  }

  @Operation(summary = "Update tenant game flags (admin)")
  @PatchMapping("/{gameId}")
  public ApiResponse<TenantGame> update(
      @PathVariable("gameId") GameId gameId, @RequestBody @Valid TenantGameUpdateRequest req) {

    var cmd =
        new UpdateTenantGameCommand(
            gameId, req.enabled(), req.displayName(), req.minStake(), req.maxStake(), req.flags());

    boolean ok = commandBus.send(cmd);
    if (!ok) {
      return ApiResponse.success(null);
    }

    Optional<TenantGame> updated = tenantGameCatalog.findByGameId(gameId);
    return ApiResponse.success(updated.orElse(null));
  }
}
