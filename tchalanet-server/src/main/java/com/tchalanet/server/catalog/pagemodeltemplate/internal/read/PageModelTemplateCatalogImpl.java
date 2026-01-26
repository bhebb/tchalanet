package com.tchalanet.server.catalog.pagemodeltemplate.internal.read;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.cache.PageModelTemplateCacheNames;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper.PageModelTemplateMapper;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateRepository;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageModelTemplateCatalogImpl implements PageModelTemplateCatalog {

    private final PageModelTemplateRepository repository;
    private final PageModelTemplateMapper mapper;

    @Override
    @Cacheable(value = PageModelTemplateCacheNames.BY_ID, key = "#id.value()")
    public Optional<PageModelTemplateView> findById(PageModelTemplateId id) {
        return repository.findById(id.value()).map(mapper::toView);
    }

    @Override
    @Cacheable(value = PageModelTemplateCacheNames.BY_LOGICAL_ID, key = "#logicalId")
    public Optional<PageModelTemplateView> findByLogicalId(String logicalId) {
        if (logicalId == null || logicalId.isBlank()) return Optional.empty();
        return repository.findFirstByLogicalId(logicalId.trim()).map(mapper::toView);
    }

    @Override
    @Cacheable(value = PageModelTemplateCacheNames.VISIBLE_LIST, key = "'visible'")
    public List<PageModelTemplateView> listVisible() {
        // RLS decides which rows are visible (GLOBAL + current tenant)
        return mapper.toViews(repository.findAllByOrderByLogicalIdAsc());
    }

    @Override
    @Cacheable(value = PageModelTemplateCacheNames.SEARCH, key = "#pageReq.cacheKey() + ':' + #logicalIdContains + ':' + #nameContains")
    public TchPage<PageModelTemplateView> search(String logicalIdContains, String nameContains, TchPageRequest pageReq) {
        Specification<PageModelTemplateEntity> spec = (root, q, cb) -> cb.conjunction();

        if (logicalIdContains != null && !logicalIdContains.isBlank()) {
            String like = "%" + logicalIdContains.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("logicalId")), like));
        }
        if (nameContains != null && !nameContains.isBlank()) {
            String like = "%" + nameContains.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), like));
        }

        var pageable = PageRequest.of(
            pageReq.pageable().getPageNumber(),
            pageReq.pageable().getPageSize(),
            pageReq.pageable().getSort()
        );

        var page = repository.findAll(spec, pageable);
        return TchPage.of(
            mapper.toViews(page.getContent()),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
