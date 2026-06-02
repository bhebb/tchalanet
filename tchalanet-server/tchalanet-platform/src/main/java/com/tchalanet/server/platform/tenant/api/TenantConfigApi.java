package com.tchalanet.server.platform.tenant.api;

import java.util.List;

import com.tchalanet.server.platform.tenant.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantIdentityRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;

public interface TenantConfigApi {

    void createTenant(CreateTenantRequest request);
    TenantConfigView getTenantById(GetTenantByIdRequest request);
    TenantConfigView getTenantByCode(GetTenantByCodeRequest request);
    List<TenantConfigView> listTenants(ListTenantsRequest request);
    void updateTenantIdentity(UpdateTenantIdentityRequest request);
    void updateTenantInternalSettings(UpdateTenantInternalSettingsRequest request);
    void activateTenant(ActivateTenantRequest request);
    void suspendTenant(SuspendTenantRequest request);
    TenantInternalCommunicationConfig getTenantCommunicationConfig(GetTenantByIdRequest request);
    TenantInternalDocumentConfig getTenantDocumentConfig(GetTenantByIdRequest request);
}
