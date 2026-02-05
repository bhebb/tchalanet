package com.tchalanet.server.features.platformadmin.game;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.GameView;
import com.tchalanet.server.common.web.api.ApiResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform-admin/games")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformAdminGameController {

  private final GameCatalog gameCatalog;

  @GetMapping("/overview")
  public ApiResponse<PlatformGameOverviewView> overview(
      @RequestParam(name = "includeInactive", required = false, defaultValue = "false")
          boolean includeInactive) {

    List<GameView> items =
        includeInactive ? gameCatalog.listAll() : gameCatalog.listActive();

    // Use efficient stats() method instead of in-memory filtering
    var stats = gameCatalog.stats();

    var view =
        new PlatformGameOverviewView(
            Instant.now(),
            new PlatformGameOverviewView.Summary(stats.total(), stats.active()),
            items);

    return ApiResponse.success(view);
  }

  public record PlatformGameOverviewView(
      Instant generatedAt,
      Summary summary,
      List<GameView> games
  ) {
    public record Summary(long total, long active) {}
  }
}
