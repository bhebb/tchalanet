package com.tchalanet.server.catalog.game.internal.web;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.game.internal.web.model.GameCreateRequest;
import com.tchalanet.server.catalog.game.internal.web.model.GameUpdateRequest;
import com.tchalanet.server.catalog.game.internal.write.GameAdminService;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for platform game admin (catalog/game).
 * Translates web requests into service-layer commands.
 */
@RestController
@RequestMapping("/platform/catalog/games")
@RequiredArgsConstructor
@PreAuthorize("hasPermission('catalog.game.manage')")
public class GameAdminController {

  private final GameAdminService gameAdminService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<GameView> create(@Valid @RequestBody GameCreateRequest request) {
    var cmd = new GameAdminService.CreateCommand(
        request.code(), request.name(), request.category(), request.combination(),
        request.minDigits(), request.maxDigits(), request.description(),
        request.active(), request.sortOrder());
    return ApiResponse.created(gameAdminService.create(cmd));
  }

  @PutMapping("/{id}")
  public ApiResponse<GameView> update(@PathVariable("id") GameId id, @Valid @RequestBody GameUpdateRequest request) {
    var cmd = new GameAdminService.UpdateCommand(
        request.name(), request.category(), request.combination(),
        request.minDigits(), request.maxDigits(), request.description(),
        request.active(), request.sortOrder());
    return ApiResponse.success(gameAdminService.update(id, cmd));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> softDelete(@PathVariable("id") GameId id) {
    gameAdminService.softDelete(id);
    return ApiResponse.success(null);
  }

  @PostMapping("/{id}/deactivate")
  public ApiResponse<Void> deactivate(@PathVariable("id") GameId id) {
    gameAdminService.deactivate(id);
    var notice = new ApiNotice(
        "GAME_DEACTIVATED",
        "Le jeu a été désactivé avec succès.",
        "game",
        NoticeSeverity.INFO,
        Map.of("gameId", id.value())
    );
    return ApiResponse.warn(null, notice);
  }
}
