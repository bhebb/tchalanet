package com.tchalanet.server.core.tenant.web;

import com.tchalanet.server.core.tenant.domain.model.TenantGame;
import com.tchalanet.server.core.tenant.domain.usecase.TenantGameCrudUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/tenant-games")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantGameAdminController {

  private final TenantGameCrudUseCase tenantGameCrud;

  @PostMapping
  public ResponseEntity<TenantGame> create(@RequestBody TenantGame t) {
    return ResponseEntity.ok(tenantGameCrud.create(t));
  }

  @GetMapping("/tenant/{tenantId}")
  public List<TenantGame> listByTenant(@PathVariable UUID tenantId) {
    return tenantGameCrud.listByTenant(tenantId);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    // deletion method left simple for now; implement soft-delete in tenantGame repository if needed
    return ResponseEntity.noContent().build();
  }
}
