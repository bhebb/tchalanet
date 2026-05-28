package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.communication.api.CommunicationSettingsApi;
import com.tchalanet.server.platform.communication.api.model.value.TenantCommunicationSettingsView;
import com.tchalanet.server.platform.communication.internal.persistence.CommunicationSettingsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CommunicationSettingsApiService implements CommunicationSettingsApi {

    private final CommunicationSettingsJpaRepository settings;

    @Override
    @Transactional(readOnly = true)
    public TenantCommunicationSettingsView getTenantSettings(TenantId tenantId) {
        return settings.findByTenantId(tenantId.value())
            .map(row -> new TenantCommunicationSettingsView(
                row.isEmailEnabled(),
                row.getCriticalAlertEmail(),
                row.getOpsAlertEmail(),
                row.getDefaultLocale()))
            .orElseGet(() -> new TenantCommunicationSettingsView(true, null, null, "fr"));
    }
}
