package com.tchalanet.server.features.pagemodel.shared.init;

import com.tchalanet.server.features.pagemodel.shared.PageModelEntity;
import com.tchalanet.server.features.pagemodel.shared.PageModelRepository;
import com.tchalanet.server.features.pagemodel.shared.PageModelType;
import com.tchalanet.server.features.pagemodel.shared.PageStatus;
import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateEntity;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PageModelBootstrapService {

    private final PageModelRepository repository;
    private final PageModelTemplateService templateService;
    private final PageModelService pageModelService;

    /**
     * Alias historique : bootstrapForTenant -> seedDefaultsForTenant
     */
    public void bootstrapForTenant(UUID tenantId) {
        seedDefaultsForTenant(tenantId);
    }

    public void seedDefaultsForDefaultTenant() {
        seedDefaultsForTenant(PageModelTenantConstants.DEFAULT_TENANT_ID);
    }

    public void seedDefaultsForTenant(UUID tenantId) {
        for (PageModelType type : PageModelType.values()) {
            var existing = repository.findAllByTenantIdAndLogicalId(tenantId, type.logicalId());
            if (existing.isEmpty()) {
                ensureTemplateAndCreateInstance(tenantId, type);
            }
        }
    }

    private void ensureTemplateAndCreateInstance(UUID tenantId, PageModelType type) {
        // 1) try to find default system template in DB
        Optional<PageModelTemplateEntity> tplOpt = templateService.findDefaultByLogicalId(type.logicalId());
        PageModelTemplateEntity tpl;
        if (tplOpt.isPresent()) {
            tpl = tplOpt.get();
        } else {
            // 2) try to load JSON template from resources and create a system template
            String path = "pagemodel/templates/default/" + type.logicalId() + ".json";
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                // nothing to seed
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                String json = new String(is.readAllBytes());
                PageModelTemplateEntity createdTpl = new PageModelTemplateEntity();
                createdTpl.setTenantId(null); // system template
                createdTpl.setLogicalId(type.logicalId());
                createdTpl.setLabel(type.slug());
                createdTpl.setDescription(null);
                createdTpl.setSchemaVersion(1);
                createdTpl.setModelJson(json);
                createdTpl.setDefault(true);
                createdTpl.setSystem(true);
                tpl = templateService.create(createdTpl, null);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read template resource: " + type.logicalId(), e);
            }
        }

        // 3) create a PUBLISHED instance for this tenant based on the template
        pageModelService.createFromTemplate(tenantId, tpl, true, null);
    }
}
