package com.tchalanet.server.platform.tenantconfig.internal.adapter;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenantconfig.internal.port.TenantConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DefaultTenantConfigReader implements TenantConfigReader {

    private final TenantPersistenceAdapter tenants;
    private final JsonUtils jsonUtils;

    @Override
    @Transactional(readOnly = true)
    public TenantInternalSettings getInternalSettings(TenantId tenantId) {
        var tenant = tenants.getRequiredByIdActive(tenantId);
        var config = tenant.config();
        if (config == null || config.isNull()) {
            return null;
        }
        return jsonUtils.treeToValue(config, TenantInternalSettings.class);
    }
}
