package com.tchalanet.server.features.pagemodel_backup.shared;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.features.pagemodel.LangResolver;
import com.tchalanet.server.features.pagemodel.PageModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PageModelRuntimeService {

  private final PageModelService pageModelService;
  private final LangResolver langResolver;
  private final PageModelDynamicResolver dynamicResolver;
  private final TenantI18nOverrideService i18nOverrideService;
  private final TchContextResolver contextResolver;

  public PageModelResponse resolvePublic(String logicalId, Optional<String> langFromUrl) {

    var holder = contextResolver.currentOrNull();
    UUID tenantId = holder != null ? holder.tenantUuid() : null;

    PageModel pageModel = pageModelService.loadEffectiveModel(tenantId, logicalId);

    var currentLang = langResolver.resolve(new LangResolver.LangResolverContext(
        langFromUrl == null ? Optional.empty() : langFromUrl,
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(pageModel.meta().defaultLang()),
        pageModel.meta().langs(),
        "en"
    ));

    var dynamic = dynamicResolver.resolve(pageModel, currentLang, holder);

    var overrides = i18nOverrideService.resolveAsMap(tenantId, currentLang);

    return new PageModelResponse(
        currentLang,
        pageModel.meta().langs(),
        pageModel,
        dynamic,
        overrides
    );
  }
}

