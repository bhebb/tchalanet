package com.tchalanet.server.core.pagemodel.infra.init;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.features.pagemodel.PageModelRepository;
import com.tchalanet.server.features.pagemodel.PageModelService;
import com.tchalanet.server.features.pagemodel.PageModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageModelBootstrapService {

    private final PageModelRepository repository;
    private final PageModelTemplateCatalog templateCatalog;
    private final PageModelService pageModelService;

    public void seedDefaultsForTenant() {

        // Fail fast: this runs under tenant context (batch/startup runner sets it)
        var ctx = TchContext.get();
        if (ctx == null || ctx.tenantIdSafe() == null) {
            throw new IllegalStateException("PageModelBootstrapService requires a tenant context (RLS).");
        }

        for (PageModelType type : PageModelType.values()) {
            try {
                boolean exists = repository.existsByLogicalIdAndDeletedAtIsNull(type.logicalId());
                if (!exists) {
                    createFromTemplate(type);
                }
            } catch (DataAccessException dae) {
                log.warn("Skipping page_model seed: DB not ready yet: {}", dae.getMessage());
                return;
            }
        }
    }

    private void createFromTemplate(PageModelType type) {
        var tplOpt = templateCatalog.findByLogicalId(type.logicalId());
        if (tplOpt.isEmpty()) {
            throw new IllegalStateException(
                "Missing PageModelTemplate for logicalId=" + type.logicalId()
                    + ". Ensure catalog seed ran (PageModelTemplateSeedRunner).");
        }

        pageModelService.createFromTemplate(tplOpt.get(), true);
        log.info("Seeded page_model from template logicalId={}", type.logicalId());
    }
}

