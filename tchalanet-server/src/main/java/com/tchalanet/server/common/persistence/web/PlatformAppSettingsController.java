package com.tchalanet.server.common.persistence.web;

import com.tchalanet.server.common.persistence.AppSettingEntity;
import com.tchalanet.server.common.persistence.service.PlatformAppSettingsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/app-settings")
@RequiredArgsConstructor
@Tag(name = "Platform • App Settings")
public class PlatformAppSettingsController {

  private final PlatformAppSettingsService service;

  @GetMapping
  public List<AppSettingEntity> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public AppSettingEntity get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<AppSettingEntity> create(@RequestBody AppSettingEntity e) {
    var created = service.create(e);
    return ResponseEntity.status(201).body(created);
  }

  @PutMapping("/{id}")
  public AppSettingEntity update(@PathVariable UUID id, @RequestBody AppSettingEntity e) {
    return service.update(id, e);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
