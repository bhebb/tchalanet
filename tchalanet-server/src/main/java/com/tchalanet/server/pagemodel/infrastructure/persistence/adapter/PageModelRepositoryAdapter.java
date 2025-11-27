package com.tchalanet.server.pagemodel.infrastructure.persistence.adapter;

import com.tchalanet.server.pagemodel.domain.model.PageModel;
import com.tchalanet.server.pagemodel.domain.ports.out.PageModelRepositoryPort;
import com.tchalanet.server.pagemodel.infrastructure.persistence.PageModelEntity;
import com.tchalanet.server.pagemodel.infrastructure.persistence.PageModelJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary; // New import
import org.springframework.stereotype.Component;

@Component
@Primary // This adapter will be preferred when PageModelRepositoryPort is injected
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
