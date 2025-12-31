package com.tchalanet.server.core.theme.infra.web;

import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import com.tchalanet.server.core.theme.infra.persistence.ThemeJpaEntity;
import com.tchalanet.server.core.theme.infra.service.PlatformThemeService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/themes")
@RequiredArgsConstructor
public class PlatformThemeController {

  private final PlatformThemeService service;

  @GetMapping
  public List<ThemeJpaEntity> list(
      @RequestParam(name = "status", required = false) ThemeStatus status) {
    return service.list(status);
  }

  @GetMapping("/{id}")
  public ThemeJpaEntity get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ThemeJpaEntity> create(@RequestBody ThemeJpaEntity t) {
    var created = service.create(t);
    return ResponseEntity.status(201).body(created);
  }

  @PutMapping("/{id}")
  public ThemeJpaEntity update(@PathVariable UUID id, @RequestBody ThemeJpaEntity t) {
    return service.update(id, t);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
