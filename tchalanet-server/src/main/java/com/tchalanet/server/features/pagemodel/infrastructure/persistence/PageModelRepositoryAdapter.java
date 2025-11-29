package com.tchalanet.server.features.pagemodel.infrastructure.persistence;

import com.tchalanet.server.features.pagemodel.domain.model.PageModel;
import com.tchalanet.server.features.pagemodel.domain.ports.out.PageModelRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageModelRepositoryAdapter implements PageModelRepositoryPort {

  private final PageModelJpaRepository jpa;

  @Override
  public Optional<PageModel> findByTenantAndCodeAndLang(UUID tenantId, String code, String lang) {
    return jpa.findFirstByTenantIdAndCodeAndLang(tenantId, code, lang).map(this::toDomain);
  }

  private PageModel toDomain(PageModelEntity e) {
    return new PageModel(e.getId(), e.getTenantId(), e.getCode(), e.getLang(), e.getJson());
  }
}
