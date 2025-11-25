package com.tchalanet.server.game.web;

import com.tchalanet.server.game.domain.model.Game;
import com.tchalanet.server.game.domain.usecase.GameCrudUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/games")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GameAdminController {

  private final GameCrudUseCase gameCrud;

  @PostMapping
  public ResponseEntity<Game> create(@RequestBody Game g) {
    return ResponseEntity.ok(gameCrud.create(g));
  }

  @GetMapping("/{code}")
  public ResponseEntity<Game> get(@PathVariable String code) {
    return gameCrud
        .get(code)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping
  public List<Game> list() {
    return gameCrud.listActive();
  }

  @PutMapping("/{code}")
  public ResponseEntity<Game> update(@PathVariable String code, @RequestBody Game g) {
    try {
      return ResponseEntity.ok(gameCrud.update(code, g));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{code}")
  public ResponseEntity<Void> delete(@PathVariable String code) {
    gameCrud.delete(code);
    return ResponseEntity.noContent().build();
  }
}
