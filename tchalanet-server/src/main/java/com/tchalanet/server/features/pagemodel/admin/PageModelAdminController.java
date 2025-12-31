package com.tchalanet.server.features.pagemodel.admin;

import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminDetailDto;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminListItemDto;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminUpsertRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/pagemodels")
@RequiredArgsConstructor
public class PageModelAdminController {

  private final PageModelAdminService service;

  /**
   * Liste les PageModel avec filtres optionnels : - tenantId : null => global / tous - scope :
   * "public" / "private" (optionnel) - logicalId: ex. "public.home", "private.tenant_dashboard"
   */
  @GetMapping
  public ResponseEntity<List<PageModelAdminListItemDto>> list(
      @RequestParam(value = "tenantId", required = false) UUID tenantId,
      @RequestParam(value = "scope", required = false) String scope,
      @RequestParam(value = "logicalId", required = false) String logicalId) {
    List<PageModelAdminListItemDto> items = service.list(tenantId, scope, logicalId);
    return ResponseEntity.ok(items);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PageModelAdminDetailDto> get(@PathVariable("id") UUID id) {
    try {
      PageModelAdminDetailDto dto = service.get(id);
      return ResponseEntity.ok(dto);
    } catch (IllegalArgumentException ex) {
      // Ou une exception métier type PageModelNotFoundException
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<PageModelAdminDetailDto> create(
      @RequestBody PageModelAdminUpsertRequest request) {
    PageModelAdminDetailDto created = service.upsert(request);
    // On suppose que le DTO contient l'ID
    URI location = URI.create("/admin/pagemodels/" + created.id());
    return ResponseEntity.created(location).body(created);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PageModelAdminDetailDto> update(
      @PathVariable("id") UUID id, @RequestBody PageModelAdminUpsertRequest request) {
    var withId =
        new PageModelAdminUpsertRequest(
            id,
            request.logicalId(),
            request.scope(),
            request.slug(),
            request.schemaVersion(),
            request.model());
    PageModelAdminDetailDto updated = service.upsert(withId);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
    try {
      service.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.notFound().build();
    }
  }

  /** Duplique un PageModel : - newLogicalId / newSlug optionnels (sinon générés par le service). */
  @PostMapping("/{id}/duplicate")
  public ResponseEntity<PageModelAdminDetailDto> duplicate(
      @PathVariable("id") UUID id,
      @RequestParam(name = "tenantId", required = false) UUID tenantId,
      @RequestParam(name = "logicalId", required = false) String newLogicalId,
      @RequestParam(name = "slug", required = false) String newSlug) {
    try {
      PageModelAdminDetailDto cloned = service.duplicate(id, tenantId, newLogicalId, newSlug);
      return ResponseEntity.ok(cloned);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Preview "brute" du PageModel tel qu'il serait renvoyé au BFF, pratique pour le backoffice
   * visuel.
   */
  @GetMapping("/{id}/preview")
  public ResponseEntity<PageModelAdminDetailDto> preview(@PathVariable("id") UUID id) {
    try {
      PageModelAdminDetailDto dto = service.preview(id);
      return ResponseEntity.ok(dto);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Publie une version (draft -> published). Règle métier: une seule version "published" par
   * logicalId/tenant/scope.
   */
  @PostMapping("/{id}/publish")
  public ResponseEntity<PageModelAdminDetailDto> publish(@PathVariable("id") UUID id) {
    try {
      PageModelAdminDetailDto dto = service.publish(id);
      return ResponseEntity.ok(dto);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.notFound().build();
    }
  }
}
