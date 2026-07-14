package com.tchalanet.server.platform.tenant.api;

import com.tchalanet.server.platform.tenant.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantInternalSettingsSectionRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantIdentityRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsSectionRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantHolidayTemplateView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSummaryView;
import java.util.List;
import tools.jackson.databind.JsonNode;

public interface TenantConfigApi {

  void createTenant(CreateTenantRequest request);

  TenantConfigView getTenantById(GetTenantByIdRequest request);

  TenantConfigView getTenantByCode(GetTenantByCodeRequest request);

  List<TenantSummaryView> listTenants(ListTenantsRequest request);

  void updateTenantIdentity(UpdateTenantIdentityRequest request);

  void updateTenantInternalSettings(UpdateTenantInternalSettingsRequest request);

  void updateTenantInternalSettingsSection(UpdateTenantInternalSettingsSectionRequest request);

  void activateTenant(ActivateTenantRequest request);

  void suspendTenant(SuspendTenantRequest request);

  TenantInternalSettings getTenantInternalSettings(GetTenantByIdRequest request);

  JsonNode getTenantInternalSettingsSection(GetTenantInternalSettingsSectionRequest request);

  TenantInternalCommunicationConfig getTenantCommunicationConfig(GetTenantByIdRequest request);

  TenantInternalDocumentConfig getTenantDocumentConfig(GetTenantByIdRequest request);

  List<TenantHolidayTemplateView> listTenantHolidayTemplates();

  TenantSettingsReadinessView getTenantSettingsReadiness(GetTenantByIdRequest request);
}
