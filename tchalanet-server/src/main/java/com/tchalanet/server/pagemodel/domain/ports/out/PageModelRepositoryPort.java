package com.tchalanet.server.pagemodel.domain.ports.out;

import com.tchalanet.server.pagemodel.domain.model.PageModel;
import java.util.Optional;
import java.util.UUID;

public interface PageModelRepositoryPort {

  Optional<PageModel> findByTenantAndCodeAndLang(UUID tenantId, String code, String lang);
}
