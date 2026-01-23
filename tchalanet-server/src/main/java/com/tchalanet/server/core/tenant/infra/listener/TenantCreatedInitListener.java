package com.tchalanet.server.core.tenant.infra.listener;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.settings.AppSettingLevel;
import com.tchalanet.server.catalog.settings.infra.persistence.AppSettingEntity;
import com.tchalanet.server.catalog.settings.infra.persistence.AppSettingRepository;
import com.tchalanet.server.catalog.settings.registry.AppSettingKey;
import com.tchalanet.server.catalog.settings.registry.AppSettingRegistry;
import com.tchalanet.server.core.tenant.domain.event.TenantCreatedEvent;
import com.tchalanet.server.features.pagemodel.shared.init.PageModelBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantCreatedInitListener {
  private final AppSettingRepository repo;
  private final PageModelBootstrapService pageModelBootstrapService;

  @EventListener
  @Transactional
  public void onTenantCreated(TenantCreatedEvent e) {
    var tenantId = e.tenantId().value();
    pageModelBootstrapService.bootstrapForDefaultTenant();

    // Seed tenant-level defaults only if missing
    createTenantSettings(e.tenantId());
  }

  // app settings
  private void createTenantSettings(TenantId tenantId) {
    for (var k : AppSettingRegistry.all()) {
      upsertIfMissing(tenantId, k);
    }
  }

  private void upsertIfMissing(TenantId tenantId, AppSettingKey<?> k) {
    // tu peux faire une query "exists" simple (ou une contrainte unique + try/catch)
    // v1 simple: insert and ignore duplicates -> nécessite un repo custom.
    var ent = new AppSettingEntity();
    ent.setLevel(AppSettingLevel.TENANT);
    ent.setTenantId(tenantId.uuid());
    ent.setNamespace(k.namespace());
    ent.setSettingKey(k.key());
    ent.setValueType(k.type());
    ent.setSettingValue(String.valueOf(k.defaultValue()));
    ent.setActive(true);

    // si tu as unique active index, tu peux catch DataIntegrityViolationException
    try {
      repo.save(ent);
    } catch (Exception exception) {
      log.error(exception.getMessage(), exception);
    }
  }
}
