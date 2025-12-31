package com.tchalanet.server.core.game.infra.rest;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.game.application.command.EnsureTenantGamesCommand;
import com.tchalanet.server.core.game.application.command.UpdateTenantGameCommand;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import com.tchalanet.server.core.game.application.query.model.FindTenantGameByIdQuery;
import com.tchalanet.server.core.game.application.query.model.ListEnabledTenantGamesQuery;
import com.tchalanet.server.core.game.application.query.model.ListTenantGamesQuery;
import com.tchalanet.server.core.game.infra.web.TenantGameUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tenant/games")
@RequiredArgsConstructor
public class TenantGameAdminController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;

  @GetMapping
  public List<TenantGame> listAll() {
    return queryBus.send(new ListTenantGamesQuery());
  }

  @GetMapping("/enabled")
  public List<TenantGame> listEnabled() {
    return queryBus.send(new ListEnabledTenantGamesQuery());
  }

  @PostMapping("/ensure")
  public ResponseEntity<?> ensure(@RequestBody(required = false) java.util.List<String> codes) {
    var res = commandBus.send(new EnsureTenantGamesCommand(codes));
    return ResponseEntity.ok().body(Map.of("created", res.createdCodes().size()));
  }

  @PatchMapping("/{gameId}")
  public ResponseEntity<?> update(
      @PathVariable UUID gameId, @RequestBody @Valid TenantGameUpdateRequest req) {

    var cmd = new UpdateTenantGameCommand(
        gameId,
        req.getEnabled(),
        req.getDisplayName(),
        req.getMinStake(),
        req.getMaxStake(),
        req.getFlags());

    boolean ok = commandBus.send(cmd);
    if (!ok) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "TenantGame not found for given gameId. Consider calling /ensure."));
    }

    Optional<TenantGame> updated = queryBus.send(new FindTenantGameByIdQuery(gameId));
    return ResponseEntity.ok(updated.orElse(null));
  }
}
