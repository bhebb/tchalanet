package com.tchalanet.server.core.game.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.game.application.command.model.EnsureTenantGamesCommand;
import com.tchalanet.server.core.game.application.command.model.UpdateTenantGameCommand;
import com.tchalanet.server.core.game.application.query.model.FindTenantGameByIdQuery;
import com.tchalanet.server.core.game.application.query.model.ListEnabledTenantGamesQuery;
import com.tchalanet.server.core.game.application.query.model.ListTenantGamesQuery;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import com.tchalanet.server.core.game.infra.web.model.EnsureTenantGamesRequest;
import com.tchalanet.server.core.game.infra.web.model.EnsureTenantGamesResponse;
import com.tchalanet.server.core.game.infra.web.model.TenantGameUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tenant/games")
@RequiredArgsConstructor
public class TenantGameAdminController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;

  @GetMapping
  public ApiResponse<List<TenantGame>> listAll() {
    var data = queryBus.send(new ListTenantGamesQuery());
    return ApiResponse.success(data);
  }

  @GetMapping("/enabled")
  public ApiResponse<List<TenantGame>> listEnabled() {
    var data = queryBus.send(new ListEnabledTenantGamesQuery());
    return ApiResponse.success(data);
  }

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

    Optional<TenantGame> updated = queryBus.send(new FindTenantGameByIdQuery(gameId.uuid()));
    return ApiResponse.success(updated.orElse(null));
  }
}
