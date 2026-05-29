package com.tchalanet.server.catalog.pagemodeltemplate.api;

import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateStatsView;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

import java.util.List;
import java.util.Optional;

public interface PageModelTemplateCatalog {

    Optional<PageModelTemplateView> findById(PageModelTemplateId id);

    Optional<PageModelTemplateView> findByLogicalId(String logicalId);

    /**
     * List templates visible under current RLS context.
     * (GLOBAL + TENANT for current tenant, based on DB policy)
     */
    List<PageModelTemplateView> listVisible();

    /**
     * Optional: paged search if you need admin screens.
     */
    TchPage<PageModelTemplateView> search(String logicalIdContains, String nameContains, TchPageRequest pageReq);

    // NEW: global stats for console
    PageModelTemplateStatsView stats();
    /**
     * Returns templates that should be instantiated for a tenant during onboarding.
     *
     * Ordering should be stable by code/logicalId.
     */
    List<PageModelTemplateView> findDefaultGlobalTemplates();
}
