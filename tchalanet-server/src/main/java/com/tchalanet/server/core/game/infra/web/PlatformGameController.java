package com.tchalanet.server.core.game.infra.web;

import com.tchalanet.server.core.game.infra.persistence.GameJpaEntity;
import com.tchalanet.server.core.game.infra.service.PlatformGameService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/games")
@RequiredArgsConstructor
@Tag(name = "Platform • Games")
public class PlatformGameController {

  private final PlatformGameService service;

  @GetMapping
  public List<GameJpaEntity> list() {
    return service.list();
  }

  @GetMapping("/by-code/{code}")
  public GameJpaEntity getByCode(@PathVariable String code) {
    return service.getByCode(code);
  }

  @GetMapping("/{id}")
  public GameJpaEntity get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<GameJpaEntity> create(@RequestBody GameJpaEntity g) {
    var created = service.create(g);
    return ResponseEntity.status(201).body(created);
  }

  @PutMapping("/{id}")
  public GameJpaEntity update(@PathVariable UUID id, @RequestBody GameJpaEntity g) {
    return service.update(id, g);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
