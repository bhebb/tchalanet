package com.tchalanet.server.platform.tenantconfig.api;

import java.util.List;

import com.tchalanet.server.platform.tenantconfig.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenantconfig.api.model.request.UpdateTenantIdentityRequest;

public interface TenantConfigApi {

    void createTenant(CreateTenantRequest request);
    TenantConfigView getTenantById(GetTenantByIdRequest request);
    TenantConfigView getTenantByCode(GetTenantByCodeRequest request);
    List<TenantConfigView> listTenants(ListTenantsRequest request);
    void updateTenantIdentity(UpdateTenantIdentityRequest request);
    void activateTenant(ActivateTenantRequest request);
    void suspendTenant(SuspendTenantRequest request);
}
