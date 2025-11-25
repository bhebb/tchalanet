package com.tchalanet.server.tenant.web;

import com.tchalanet.server.tenant.domain.model.Plan;
import com.tchalanet.server.tenant.domain.usecase.PlanCrudUseCase;
import com.tchalanet.server.tenant.domain.usecase.subscription.ChangePlanUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlanAdminController {

  private final PlanCrudUseCase planCrud;
  private final ChangePlanUseCase changePlanUseCase;

  @PostMapping
  public ResponseEntity<Plan> create(@RequestBody Plan p) {
    return ResponseEntity.ok(planCrud.create(p));
  }

  @GetMapping("/{code}")
  public ResponseEntity<Plan> get(@PathVariable String code) {
    return planCrud
        .getByCode(code)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping
  public List<Plan> list() {
    return planCrud.listAll();
  }

  @PutMapping("/{code}")
  public ResponseEntity<Plan> update(@PathVariable String code, @RequestBody Plan p) {
    try {
      return ResponseEntity.ok(planCrud.update(code, p));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{code}")
  public ResponseEntity<Void> delete(@PathVariable String code) {
    planCrud.delete(code);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{code}/assign/{tenantId}")
  public ResponseEntity<?> assignPlanToTenant(
      @PathVariable String code, @PathVariable UUID tenantId) {
    // find plan by code to obtain planId
    var opt = planCrud.getByCode(code);
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    var plan = opt.get();

    var req =
        new com.tchalanet.server.tenant.web.dto.ChangePlanRequest(
            plan.id(), false, UUID.randomUUID().toString());
    try {
      var dto = changePlanUseCase.execute(tenantId, req);
      return ResponseEntity.ok(dto);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
