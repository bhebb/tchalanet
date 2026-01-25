package com.tchalanet.server.catalog.pagemodeltemplate.api;

import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;
import java.util.Optional;

public interface PageModelTemplateCatalog {

  Optional<PageModelTemplateView> findById(PageModelTemplateId id);

  Optional<PageModelTemplateView> findSystemDefaultByLogicalId(String logicalId);

  List<PageModelTemplateView> listByTenant(TenantId tenantId);

  List<PageModelTemplateView> listSystemTemplates();
}
