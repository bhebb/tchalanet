package com.tchalanet.server.catalog.pagemodeltemplate.internal.write;

import com.tchalanet.server.catalog.pagemodeltemplate.api.event.PageModelTemplateUpdatedEvent;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.cache.PageModelTemplateCacheNames;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper.PageModelTemplateMapper;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateRepository;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PageModelTemplateAdminService {

    private final PageModelTemplateRepository repository;
    private final PageModelTemplateMapper mapper;
    private final JsonUtils jsonUtils;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public PageModelTemplateView createFromView(PageModelTemplateView view) {
        // enforce unique logicalId among non-deleted rows
        repository.findFirstByLogicalIdAndDeletedAtIsNull(view.logicalId())
            .ifPresent(x -> {
                throw ProblemRest.conflict("page_model_template.logical_id" + view.logicalId());
            });

        PageModelTemplateEntity e = new PageModelTemplateEntity();
        mapper.applyView(e, view);
        var saved = repository.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public PageModelTemplateView updateFromView(PageModelTemplateId id, PageModelTemplateView view, UserId actorId) {
        var existing = repository.findById(id.value())
            .orElseThrow(() -> ProblemRest.notFound("page_model_template", id));

        // if logicalId changes, enforce unique among non-deleted rows
        if (view.logicalId() != null && !view.logicalId().equals(existing.getLogicalId())) {
            repository.findFirstByLogicalIdAndDeletedAtIsNull(view.logicalId())
                .ifPresent(x -> {
                    throw ProblemRest.conflict("page_model_template.logical_id" + view.logicalId());
                });
        }

        mapper.applyView(existing, view);
        existing.setUpdatedAt(Instant.now());
        var saved = repository.save(existing);
        var schemaVersion = view.schemaVersion() != null ? view.schemaVersion() : saved.getSchemaVersion();

        // ADR-EXCEPTION: catalog.pagemodeltemplate is allowed to publish an application event here.
        // Rationale: core.pagemodel must be notified synchronously after commit when a template
        // is updated so affected tenant drafts can be refreshed. The event is purely a notification
        // (not a domain event from a business invariant) and is consumed only by core.pagemodel.
        // Documented in: tchalanet-docs/docs/03-adr/adr-001-pagemodeltemplate-catalog-event.md
        AfterCommit.run(() -> eventPublisher.publishEvent(new PageModelTemplateUpdatedEvent(
            id,
            saved.getLogicalId(),
            view.model(),
            schemaVersion == null ? 1 : schemaVersion,
            actorId,
            Instant.now()
        )));

        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public void softDelete(PageModelTemplateId id) {
        var existing = repository.findById(id.value())
            .orElseThrow(() -> ProblemRest.notFound("page_model_template", id));
        existing.setDeletedAt(Instant.now());
        repository.save(existing);
    }

    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public PageModelTemplateView setDefault(PageModelTemplateId id) {
        var existing = repository.findById(id.value())
            .orElseThrow(() -> ProblemRest.notFound("page_model_template", id));
        existing.setDefault(true);
        var saved = repository.save(existing);
        return mapper.toView(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST
    }, allEntries = true)
    public PageModelTemplateView upsertGlobalFromSeed(PageModelTemplateView seed) {

        if (seed.logicalId() == null || seed.logicalId().isBlank()) {
            throw new IllegalArgumentException("Seed template requires logicalId");
        }
        if (seed.scope() == null || seed.scope().isBlank()) {
            throw new IllegalArgumentException("Seed template requires scope for logicalId=" + seed.logicalId());
        }
        if (seed.slug() == null || seed.slug().isBlank()) {
            throw new IllegalArgumentException("Seed template requires slug for logicalId=" + seed.logicalId());
        }

        var existingOpt = repository.findFirstByLogicalIdAndDeletedAtIsNull(seed.logicalId());

        PageModelTemplateEntity e;
        if (existingOpt.isEmpty()) {
            e = new PageModelTemplateEntity();
            e.setLogicalId(seed.logicalId());
            e.setCode((seed.code() == null || seed.code().isBlank()) ? seed.logicalId() : seed.code());
            e.setName((seed.name() == null || seed.name().isBlank()) ? seed.logicalId() : seed.name());
        } else {
            e = existingOpt.get();
        }

        // force GLOBAL
        e.setLevel(PageModelTemplateLevel.GLOBAL);
        e.setTenantId(null);

        // required routing/runtime metadata
        e.setScope(seed.scope().trim());
        e.setSlug(seed.slug().trim());

        // update fields
        e.setLabel(seed.label());
        e.setDescription(seed.description());
        e.setSchema(jsonUtils.toJson(seed.schema()));
        e.setModel(jsonUtils.toJson(seed.model()));
        e.setSchemaVersion(seed.schemaVersion() == null || seed.schemaVersion() <= 0 ? 1 : seed.schemaVersion());
        e.setDefault(seed.isDefault());

        var saved = repository.save(e);

        return mapper.toView(saved);
    }

    public PageModelTemplateView preview(PageModelTemplateId id) {
        return repository.findById(id.value())
            .map(mapper::toView)
            .orElseThrow(() -> ProblemRest.notFound("page_model_template", id));
    }

    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public PageModelTemplateView duplicate(PageModelTemplateId id, String newLogicalId, String newCode) {
        var source = repository.findById(id.value())
            .orElseThrow(() -> ProblemRest.notFound("page_model_template", id));

        String targetLogicalId = (newLogicalId != null && !newLogicalId.isBlank())
            ? newLogicalId : source.getLogicalId() + "-copy";
        String targetCode = (newCode != null && !newCode.isBlank())
            ? newCode : source.getCode() + "-copy";

        repository.findFirstByLogicalIdAndDeletedAtIsNull(targetLogicalId)
            .ifPresent(x -> {
                throw ProblemRest.conflict("page_model_template.logical_id " + targetLogicalId);
            });

        var copy = createNewModelTemplateEntity(targetCode, targetLogicalId, source);
        var saved = repository.save(copy);
        return mapper.toView(saved);
    }

    private static @NonNull PageModelTemplateEntity createNewModelTemplateEntity(String targetCode, String targetLogicalId, PageModelTemplateEntity source) {
        var copy = new PageModelTemplateEntity();
        copy.setCode(targetCode);
        copy.setLogicalId(targetLogicalId);
        copy.setScope(source.getScope());
        copy.setSlug(source.getSlug());
        copy.setName(source.getName() + " (copy)");
        copy.setLabel(source.getLabel());
        copy.setDescription(source.getDescription());
        copy.setSchema(source.getSchema());
        copy.setModel(source.getModel());
        copy.setSchemaVersion(source.getSchemaVersion());
        copy.setDefault(false);
        copy.setLevel(source.getLevel());
        copy.setTenantId(source.getTenantId());
        return copy;
    }

    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public PageModelTemplateView resetToDefaults(PageModelTemplateId id) {
        var existing = repository.findById(id.value())
            .orElseThrow(() -> ProblemRest.notFound("page_model_template", id));

        existing.setModel("{}");
        existing.setSchema("{}");
        existing.setSchemaVersion(1);
        existing.setUpdatedAt(Instant.now());

        var saved = repository.save(existing);
        return mapper.toView(saved);
    }
}
