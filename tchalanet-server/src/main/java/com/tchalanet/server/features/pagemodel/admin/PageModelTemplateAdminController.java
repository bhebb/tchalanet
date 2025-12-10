package com.tchalanet.server.features.pagemodel.admin;

import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateEntity;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateMapper;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateService;
import com.tchalanet.server.features.pagemodel.shared.template.dto.PageModelTemplateRequest;
import com.tchalanet.server.features.pagemodel.shared.template.dto.PageModelTemplateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/page-model-templates")
@RequiredArgsConstructor
public class PageModelTemplateAdminController {

    private final PageModelTemplateService service;
    private final PageModelTemplateMapper mapper;

    /**
     * Liste les templates :
     * - tenantId null => templates système (par défaut)
     * - tenantId non null => templates spécifiques tenant
     */
    @GetMapping
    public ResponseEntity<List<PageModelTemplateResponse>> list(
        @RequestParam(value = "tenantId", required = false) UUID tenantId
    ) {
        List<PageModelTemplateEntity> entities = (tenantId == null)
            ? service.findAllSystemTemplates()
            : service.findAllByTenant(tenantId);

        List<PageModelTemplateResponse> resp = entities.stream()
            .map(mapper::toResponse)
            .toList();

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageModelTemplateResponse> getById(@PathVariable UUID id) {
        return service.findById(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PageModelTemplateResponse> create(
        @RequestBody PageModelTemplateRequest req,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        PageModelTemplateEntity created = service.create(mapper.toEntity(req), userId);
        URI location = URI.create("/api/admin/page-model-templates/" + created.getId());
        return ResponseEntity
            .created(location)
            .body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PageModelTemplateResponse> update(
        @PathVariable UUID id,
        @RequestBody PageModelTemplateRequest dto,
        @RequestParam(value = "propagate", required = false, defaultValue = "false") boolean propagate,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        try {
            PageModelTemplateEntity updated = propagate
                ? service.updateAndPropagate(id, mapper.toEntity(dto), userId, true)
                : service.update(id, mapper.toEntity(dto), userId);

            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        try {
            service.softDelete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<PageModelTemplateResponse> setDefault(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        try {
            PageModelTemplateEntity updated = service.setDefault(id, userId);
            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
