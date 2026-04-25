package com.tchalanet.server.core.pagemodel.infra.web;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminDetailDto;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper infra : PageModelInstance (domaine) → PageModelAdminDetailDto (DTO admin web).
 * Centralisé ici pour éviter le mapping inline dans chaque handler/controller.
 * Conforme au pattern zéro-couplage : les handlers application injectent ce bean via l'infra.
 *
 * Note: les IDs UUID sont exposés en String dans le DTO (sans typage métier côté JSON).
 */
@Component
public class PageModelAdminMapper {

  public PageModelAdminDetailDto toAdminDetailDto(PageModelInstance inst) {
    if (inst == null) return null;
    return new PageModelAdminDetailDto(
        inst.id().toString(),
        inst.tenantId() != null ? inst.tenantId().toString() : null,
        inst.logicalId(),
        inst.scope(),
        inst.slug(),
        inst.status(),
        inst.schemaVersion(),
        inst.modelJson(),
        inst.templateId().map(UUID::toString).orElse(null),
        inst.createdAt(),
        inst.updatedAt(),
        inst.createdBy() != null ? inst.createdBy().toString() : null,
        inst.updatedBy() != null ? inst.updatedBy().toString() : null,
        inst.publishedAt().orElse(null)
    );
  }
}

