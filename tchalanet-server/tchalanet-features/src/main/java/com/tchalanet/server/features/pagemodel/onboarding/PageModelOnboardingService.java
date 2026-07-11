package com.tchalanet.server.features.pagemodel.onboarding;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.api.command.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageModelOnboardingService {

    private final PageModelTemplateCatalog templateCatalog;
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final JsonUtils objectMapper;

    @Transactional
    public void seedDefaults(TenantId tenantId) {
        for (PageModelTemplateView tpl : templateCatalog.findDefaultGlobalTemplates()) {
            try {
                if (pageModelExists(tenantId, tpl.logicalId())) {
                    log.debug("PageModel already exists for logicalId={}", tpl.logicalId());
                    continue;
                }
                seedFromTemplate(tpl, tenantId);
            } catch (Exception e) {
                log.warn("Skipping PageModel seed for logicalId={}: {}", tpl.logicalId(), e.getMessage(), e);
            }
        }
    }

    private boolean pageModelExists(TenantId tenantId, String logicalId) {
        try {
            queryBus.ask(new ResolveEffectivePageModelQuery(Optional.of(tenantId), logicalId));
            return true;
        } catch (TchNotFoundException e) {
            return false;
        }
    }

    private void seedFromTemplate(PageModelTemplateView tpl, TenantId tenantId) {
        var cmd = new UpsertPageModelCommand(
            Optional.empty(),
            tenantId,
            null,
            requireNonBlank(tpl.logicalId(), "logicalId"),
            requireNonBlank(tpl.scope(), "scope"),
            requireNonBlank(tpl.slug(), "slug"),
            tpl.schemaVersion() != null ? tpl.schemaVersion() : 1,
            normalizeModel(tpl.model()),
            Optional.ofNullable(tpl.id()),
            true
        );

        commandBus.execute(cmd);
        log.info("Seeded PageModel from template logicalId={} for tenant={}", tpl.logicalId(), tenantId);
    }

    private JsonNode normalizeModel(JsonNode node) {
        if (node != null && node.isTextual()) {
            return objectMapper.parse(node.asText());
        }
        return node;
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PageModelTemplate missing required field: " + field);
        }
        return value;
    }

    @Transactional
    public void seedDefaultsForDefaultTenant() {
        seedDefaults(TenantId.of(CommonConstants.DEFAULT_TENANT_UUID));
    }
}
