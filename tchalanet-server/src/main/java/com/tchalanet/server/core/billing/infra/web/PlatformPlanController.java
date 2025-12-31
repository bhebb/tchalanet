package com.tchalanet.server.core.billing.infra.web;

import com.tchalanet.server.core.billing.infra.persistence.PlanJpaEntity;
import com.tchalanet.server.core.billing.infra.service.PlatformPlanService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/plans")
@RequiredArgsConstructor
public class PlatformPlanController {

  private final PlatformPlanService service;

  @GetMapping
  public List<PlanJpaEntity> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public PlanJpaEntity getById(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<PlanJpaEntity> create(@RequestBody PlanJpaEntity p) {
    var created = service.create(p);
    return ResponseEntity.status(201).body(created);
  }

  @PutMapping("/{id}")
  public PlanJpaEntity update(@PathVariable UUID id, @RequestBody PlanJpaEntity p) {
    return service.update(id, p);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
