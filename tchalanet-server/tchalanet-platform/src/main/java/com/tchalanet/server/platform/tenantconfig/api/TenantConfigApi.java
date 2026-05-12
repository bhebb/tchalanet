package com.tchalanet.server.platform.tenantconfig.api;

import java.util.List;

import com.tchalanet.server.platform.tenantconfig.api.model.ActivateTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.CreateTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.GetTenantByCodeQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.GetTenantByIdQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.ListTenantsQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.SuspendTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.TenantConfigView;

public interface TenantConfigApi {

    void createTenant(CreateTenantCommand request);
    TenantConfigView getTenantById(GetTenantByIdQuery request);
    TenantConfigView getTenantByCode(GetTenantByCodeQuery request);
    List<TenantConfigView> listTenants(ListTenantsQuery request);
    void activateTenant(ActivateTenantCommand request);
    void suspendTenant(SuspendTenantCommand request);
}
