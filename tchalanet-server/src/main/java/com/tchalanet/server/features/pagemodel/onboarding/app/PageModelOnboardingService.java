package com.tchalanet.server.features.pagemodel.onboarding.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.application.command.model.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace core/pagemodel/infra/init/PageModelBootstrapService (violation hexagonale).
 * Orchestre le seed initial des PageModel depuis les templates catalog.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PageModelOnboardingService {

  private final PageModelReadPort readPort;
  private final PageModelTemplateCatalog templateCatalog;
  private final CommandBus commandBus;
  private final JsonUtils objectMapper;

  @Transactional
  public void seedDefaults(TenantId tenantId) {
    for (PageModelType type : PageModelType.values()) {
      try {
        var existing = readPort.findPublishedByLogicalId(type.logicalId());
        if (existing.isPresent()) {
          log.debug("PageModel already exists for logicalId={}", type.logicalId());
          continue;
        }
        seedFromTemplate(type, tenantId);
      } catch (Exception e) {
        log.warn("Skipping PageModel seed for logicalId={}: {}", type.logicalId(), e.getMessage());
      }
    }
  }

  private void seedFromTemplate(PageModelType type, TenantId tenantId) {
    var tplOpt = templateCatalog.findByLogicalId(type.logicalId());
    if (tplOpt.isEmpty()) {
      log.warn("Missing PageModelTemplate for logicalId={}. Seed skipped.", type.logicalId());
      return;
    }
    var tpl = tplOpt.get();
    var cmd = new UpsertPageModelCommand(
        Optional.empty(),
        tenantId,   // [cascade Phase 2A-1] TenantId direct (non-null), plus Optional<TenantId>
        null,       // actorId null acceptable pour seed système (pas d'utilisateur humain)
        type.logicalId(),
        type.scope(),
        type.slug(),
        tpl.schemaVersion() != null ? tpl.schemaVersion() : 1,
        tpl.model(),
        Optional.ofNullable(tpl.id() != null ? tpl.id().toString() : null)
    );
    commandBus.send(cmd);
    log.info("Seeded PageModel from template logicalId={} for tenant={}", type.logicalId(), tenantId);
  }

  /** Seed defaults for the DEFAULT tenant (startup use case). */
  @Transactional
  public void seedDefaultsForDefaultTenant() {
    seedDefaults(TenantId.of(CommonConstants.DEFAULT_TENANT_UUID));
  }
}

