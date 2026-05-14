package com.tchalanet.server.catalog.game.internal.web;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.game.internal.web.model.GameCreateRequest;
import com.tchalanet.server.catalog.game.internal.web.model.GameUpdateRequest;
import com.tchalanet.server.catalog.game.internal.write.GameAdminService;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for platform game admin (catalog/game).
 * Conforme à 75-catalog-rules.md et REFACTORING_GUIDE.md.
 * Maps to spec requirement G2 (admin writes).
 */
@RestController
@RequestMapping("/platform/games")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class GameAdminController {

  private final GameAdminService gameAdminService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<GameView> create(@RequestBody GameCreateRequest request) {
    var view = gameAdminService.create(request);
    return ApiResponse.created(view);
  }

  @PutMapping("/{id}")
  public ApiResponse<GameView> update(@PathVariable("id") GameId id, @RequestBody GameUpdateRequest request) {
    var view = gameAdminService.update(id, request);
    return ApiResponse.success(view);
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
