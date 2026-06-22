package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantIdentityRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSummaryView;
import com.tchalanet.server.platform.tenant.internal.service.TenantConfigService;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter implementing {@link TenantConfigApi} — bridges the public API contract
 * to the internal {@link TenantConfigService}.
 */
@Component
@Primary
@RequiredArgsConstructor
public class TenantApiAdapter implements TenantConfigApi {

    private final TenantConfigService service;

    @Override
    public void createTenant(CreateTenantRequest request) {
        service.createTenant(request);
    }

    @Override
    public TenantConfigView getTenantById(GetTenantByIdRequest request) {
        return service.getTenantById(request);
    }

    @Override
    public TenantConfigView getTenantByCode(GetTenantByCodeRequest request) {
        return service.getTenantByCode(request);
    }

    @Override
    public List<TenantSummaryView> listTenants(ListTenantsRequest request) {
        return service.listTenants(request).items();
    }

    @Override
    public void updateTenantIdentity(UpdateTenantIdentityRequest request) {
        service.updateTenantIdentity(request);
    }

    @Override
    public void updateTenantInternalSettings(UpdateTenantInternalSettingsRequest request) {
        service.updateTenantInternalSettings(request);
    }

    @Override
    public void activateTenant(ActivateTenantRequest request) {
        service.activateTenant(request);
    }

    @Override
    public void suspendTenant(SuspendTenantRequest request) {
        service.suspendTenant(request);
    }

    @Override
    public TenantInternalCommunicationConfig getTenantCommunicationConfig(GetTenantByIdRequest request) {
        return service.getTenantCommunicationConfig(request);
    }

    @Override
    public TenantInternalDocumentConfig getTenantDocumentConfig(GetTenantByIdRequest request) {
        return service.getTenantDocumentConfig(request);
    }
}
