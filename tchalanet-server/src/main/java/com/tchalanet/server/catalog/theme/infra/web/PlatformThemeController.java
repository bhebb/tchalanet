package com.tchalanet.server.catalog.theme.infra.web;

import com.tchalanet.server.catalog.theme.domain.model.ThemeStatus;
import com.tchalanet.server.catalog.theme.infra.persistence.ThemeJpaEntity;
import com.tchalanet.server.catalog.theme.infra.service.PlatformThemeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/themes")
@RequiredArgsConstructor
@Tag(name = "Platform • Themes")
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
