package com.tchalanet.server.features.pagemodel.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.features.pagemodel.application.admin.PageModelDetailsDto;
import com.tchalanet.server.features.pagemodel.application.admin.PageModelPageDto;
import com.tchalanet.server.features.pagemodel.application.admin.PageModelSummaryDto;
import com.tchalanet.server.features.pagemodel.application.admin.UpsertPageModelRequest;
import com.tchalanet.server.features.pagemodel.infrastructure.persistence.PageModelAdminJpaRepository;
import com.tchalanet.server.features.pagemodel.infrastructure.persistence.PageModelEntity;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/page-models")
public class PageModelAdminController {

  private final PageModelAdminJpaRepository repo;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public PageModelAdminController(PageModelAdminJpaRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  @RequiresPermission("pages:manage")
  public PageModelPageDto list(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String lang,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    int normalizedSize = Math.max(1, Math.min(size, 200));
    Pageable pageable = PageRequest.of(page, normalizedSize);
    Page<PageModelEntity> result = repo.search(tenantId, code, lang, pageable);
    var content = result.getContent().stream().map(this::toSummary).toList();
    return new PageModelPageDto(
        content,
        result.getTotalPages(),
        result.getTotalElements(),
        result.getSize(),
        result.getNumber());
  }

  @GetMapping("/{id}")
  @RequiresPermission("pages:manage")
  public ResponseEntity<PageModelDetailsDto> get(@PathVariable UUID id) {
    Optional<PageModelEntity> opt = repo.findById(id);
    if (opt.isEmpty() || opt.get().getDeletedAt() != null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toDetails(opt.get()));
  }

  @PostMapping
  @RequiresPermission("pages:manage")
  public ResponseEntity<Object> create(@RequestBody UpsertPageModelRequest request) {
    // Validation basique
    if (request.code() == null
        || request.code().isBlank()
        || request.lang() == null
        || request.lang().isBlank()
        || request.json() == null
        || request.json().isBlank()) {
      return ResponseEntity.badRequest().body("code, lang et json sont obligatoires");
    }

    // Valider JSON
    if (!isValidJson(request.json())) {
      return ResponseEntity.badRequest().body("json invalide");
    }

    UUID tenantId = request.tenantId();
    if (repo.existsByTenantIdAndCodeAndLang(tenantId, request.code(), request.lang())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("PageModel déjà existant pour ce tenant/code/lang");
    }

    PageModelEntity entity = new PageModelEntity();
    entity.setTenantId(tenantId);
    entity.setCode(request.code());
    entity.setLang(request.lang());
    entity.setJson(request.json());

    var saved = repo.save(entity);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDetails(saved));
  }

  @PutMapping("/{id}")
  @RequiresPermission("pages:manage")
  public ResponseEntity<Object> update(
      @PathVariable UUID id, @RequestBody UpsertPageModelRequest request) {
    Optional<PageModelEntity> opt = repo.findById(id);
    if (opt.isEmpty() || opt.get().getDeletedAt() != null) {
      return ResponseEntity.notFound().build();
    }

    if (request.json() == null || request.json().isBlank()) {
      return ResponseEntity.badRequest().body("json est obligatoire");
    }
    if (!isValidJson(request.json())) {
      return ResponseEntity.badRequest().body("json invalide");
    }

    // On garde tenantId/code/lang stables, on ne met à jour que le JSON
    PageModelEntity entity = opt.get();
    entity.setJson(request.json());
    var saved = repo.save(entity);
    return ResponseEntity.ok(toDetails(saved));
  }

  @DeleteMapping("/{id}")
  @RequiresPermission("pages:manage")
  public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
    Optional<PageModelEntity> opt = repo.findById(id);
    if (opt.isEmpty() || opt.get().getDeletedAt() != null) {
      return ResponseEntity.notFound().build();
    }
    PageModelEntity entity = opt.get();
    entity.setDeletedAt(Instant.now());
    repo.save(entity);
    return ResponseEntity.noContent().build();
  }

  private boolean isValidJson(String json) {
    try {
      JsonNode node = objectMapper.readTree(json);
      return node != null;
    } catch (IOException ignored) {
      return false;
    }
  }

  private PageModelSummaryDto toSummary(PageModelEntity entity) {
    return new PageModelSummaryDto(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.getLang(),
        entity.getUpdatedAt());
  }

  private PageModelDetailsDto toDetails(PageModelEntity e) {
    return new PageModelDetailsDto(
        e.getId(),
        e.getTenantId(),
        e.getCode(),
        e.getLang(),
        e.getJson(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
