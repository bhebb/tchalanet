package com.tchalanet.server.features.pagemodel.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminDetailDto;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminListItemDto;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminUpsertRequest;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.PageModelEntity;
import com.tchalanet.server.features.pagemodel.shared.PageModelRepository;
import com.tchalanet.server.features.pagemodel.shared.PageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PageModelAdminService {

    private final PageModelRepository repository;
    private final ObjectMapper objectMapper;
    private final TchRequestContextHolder requestContextHolder;

    public List<PageModelAdminListItemDto> list(UUID tenantId, String scope, String logicalId) {
        return repository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
            .map(this::toListItemDto)
            .collect(Collectors.toList());
    }

    public PageModelAdminDetailDto get(UUID id) {
        UUID tenantId = requestContextHolder.get().tenantUuid();
        PageModelEntity entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
        return toDetailDto(entity);
    }

    /**
     * Charge un PageModel d'administration par logicalId ("type") pour le tenant courant.
     * Utile si on veut travailler "par type" de page.
     */
    public PageModelAdminDetailDto getByLogicalId(String logicalId) {
        UUID tenantId = requestContextHolder.get().tenantUuid();
        PageModelEntity entity = repository.findByTenantIdAndLogicalIdAndDeletedAtIsNull(tenantId, logicalId)
            .orElseThrow(() -> new IllegalArgumentException(
                "PageModel not found for tenant/logicalId: " + tenantId + "/" + logicalId));
        return toDetailDto(entity);
    }

    public PageModelAdminDetailDto upsert(PageModelAdminUpsertRequest request) {
        UUID tenantId = requestContextHolder.get().tenantUuid();
        Instant now = Instant.now();

        PageModelEntity entity = request.id() != null
            ? repository.findByIdAndTenantIdAndDeletedAtIsNull(request.id(), tenantId)
            .orElseGet(PageModelEntity::new)
            : new PageModelEntity();

        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(now);
            entity.setTenantId(tenantId);
            entity.setStatus(PageStatus.DRAFT);
        }

        entity.setLogicalId(request.logicalId());
        entity.setScope(request.scope());
        entity.setSlug(request.slug());
        entity.setSchemaVersion(request.schemaVersion());
        entity.setModel(serializeModel(request.model()));
        entity.setUpdatedAt(now);
        entity.setDeletedAt(null); // un upsert "réactive" éventuellement une page supprimée

        PageModelEntity saved = repository.save(entity);
        return toDetailDto(saved);
    }

    public void delete(UUID id) {
        UUID tenantId = requestContextHolder.get().tenantUuid();
        PageModelEntity entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
        Instant now = Instant.now();
        entity.setDeletedAt(now);
        entity.setUpdatedAt(now);
        repository.save(entity);
    }

    public PageModelAdminDetailDto duplicate(UUID id, UUID tenantId, String newLogicalId, String newSlug) {

        var source = repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));

        var clone = new PageModelEntity();
        clone.setTenantId(tenantId);
        clone.setLogicalId(newLogicalId != null ? newLogicalId : source.getLogicalId() + "_copy");
        clone.setScope(source.getScope());
        clone.setSlug(newSlug != null ? newSlug : source.getSlug() + "-copy");
        clone.setSchemaVersion(source.getSchemaVersion());
        clone.setModel(source.getModel());
        clone.setStatus(PageStatus.DRAFT);
        clone.setDeletedAt(null);

        var saved = repository.save(clone);
        return toDetailDto(saved);
    }

    public PageModelAdminDetailDto preview(UUID id) {
        UUID tenantId = requestContextHolder.get().tenantUuid();
        PageModelEntity entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
        return toDetailDto(entity);
    }

    private String serializeModel(PageModel model) {
        try {
            return objectMapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize PageModel", e);
        }
    }

    private PageModelAdminListItemDto toListItemDto(PageModelEntity e) {
        PageModel model = deserializeModel(e.getModel());
        String title = model.meta() != null ? model.meta().id() : e.getLogicalId();
        return new PageModelAdminListItemDto(
            e.getId(),
            e.getLogicalId(),
            e.getScope(),
            e.getSlug(),
            e.getSchemaVersion(),
            title,
            model.meta() != null ? model.meta().langs() : List.of(),
            e.getUpdatedAt()
        );
    }

    private PageModelAdminDetailDto toDetailDto(PageModelEntity e) {
        PageModel model = deserializeModel(e.getModel());
        return new PageModelAdminDetailDto(
            e.getId(),
            e.getTenantId(),
            e.getLogicalId(),
            e.getScope(),
            e.getSlug(),
            e.getSchemaVersion(),
            model,
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private PageModel deserializeModel(String json) {
        try {
            return objectMapper.readValue(json, PageModel.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize PageModel", e);
        }
    }

    public PageModelAdminDetailDto publish(UUID id) {
        var tenantId = requestContextHolder.get().tenantUuid();
        var entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
        entity.setStatus(PageStatus.PUBLISHED);
        var saved = repository.save(entity);
        return toDetailDto(saved);
    }
}
