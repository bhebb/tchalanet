package com.tchalanet.server.features.i18n;

import com.tchalanet.server.common.persistence.I18nOverrideEntity;
import com.tchalanet.server.common.persistence.I18nOverrideRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantI18nOverrideService {

  private final I18nOverrideRepository repository;

  /** Page d'overrides d'un tenant (toutes locales). */
  public Page<I18nOverrideEntity> pageByTenant(UUID tenantId, Pageable pageable) {
    return repository.findByTenantId(tenantId, pageable);
  }

  /** Page d'overrides d'un tenant pour une locale donnée ('fr', 'en', 'ht'). */
  public Page<I18nOverrideEntity> pageByTenantAndLocale(
      UUID tenantId, String locale, Pageable pageable) {

    String normalized = locale.toLowerCase(Locale.ROOT);
    return repository.findByTenantIdAndLocaleIgnoreCase(tenantId, normalized, pageable);
  }
}
