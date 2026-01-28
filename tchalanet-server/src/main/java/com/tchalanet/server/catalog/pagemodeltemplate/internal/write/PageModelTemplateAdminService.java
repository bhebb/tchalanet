package com.tchalanet.server.catalog.pagemodeltemplate.internal.write;

import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.cache.PageModelTemplateCacheNames;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper.PageModelTemplateMapper;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateRepository;
import com.tchalanet.server.common.error.NotFoundException;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PageModelTemplateAdminService {

    private final PageModelTemplateRepository repository;
    private final PageModelTemplateMapper mapper;
    private final JsonUtils jsonUtils;


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
    public PageModelTemplateView updateFromView(PageModelTemplateId id, PageModelTemplateView view) {
        var existing = repository.findById(id.value())
            .orElseThrow(() -> new NotFoundException("page_model_template" + id));

        // if logicalId changes, enforce unique among non-deleted rows
        if (view.logicalId() != null && !view.logicalId().equals(existing.getLogicalId())) {
            repository.findFirstByLogicalIdAndDeletedAtIsNull(view.logicalId())
                .ifPresent(x -> {
                    throw ProblemRest.conflict("page_model_template.logical_id" + view.logicalId());
                });
        }

        mapper.applyView(existing, view);
        existing.setUpdatedAt(Instant.now()); // optional, trigger does it too
        var saved = repository.save(existing);
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
            .orElseThrow(() -> new NotFoundException("page_model_template" + id));
        existing.setDeletedAt(Instant.now());
        repository.save(existing);
    }

    /**
     * If you keep isDefault, make it "only one default per logicalId" (but logicalId is unique → redundant).
     * You can keep it for future split (if you later relax logicalId uniqueness).
     */
    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST,
        PageModelTemplateCacheNames.SEARCH
    }, allEntries = true)
    public PageModelTemplateView setDefault(PageModelTemplateId id) {
        var existing = repository.findById(id.value())
            .orElseThrow(() -> new NotFoundException("page_model_template" + id));
        existing.setDefault(true);
        var saved = repository.save(existing);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.VISIBLE_LIST
    }, allEntries = true)
    public PageModelTemplateView upsertGlobalFromSeed(PageModelTemplateView seed) {

        if (seed.logicalId() == null || seed.logicalId().isBlank()) {
            throw new IllegalArgumentException("Seed template requires logicalId");
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

        // update fields
        e.setLabel(seed.label());
        e.setDescription(seed.description());
        e.setSchema(jsonUtils.toJson(seed.schema()));
        e.setModel(jsonUtils.toJson(seed.model()));
        e.setSchemaVersion(seed.schemaVersion() <= 0 ? 1 : seed.schemaVersion());
        e.setDefault(seed.isDefault());

        var saved = repository.save(e);
        return mapper.toView(saved);
    }


}
