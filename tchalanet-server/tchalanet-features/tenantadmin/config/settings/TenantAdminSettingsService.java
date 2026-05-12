package com.tchalanet.server.features.tenantadmin.config.settings;

import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.internal.web.model.SearchSettingsCriteria;
import com.tchalanet.server.catalog.settings.internal.write.SettingsAdminService;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantAdminSettingsService {

  private final SettingsAdminService settingsAdmin;

  public List<com.tchalanet.server.features.tenantadmin.config.settings.model.AdminSettingRow> search(TchRequestContext ctx, String namespace, String settingKey, Boolean active, TchPageRequest pageRequest) {
    var criteria = new SearchSettingsCriteria(namespace, settingKey, SettingLevel.TENANT, ctx.tenantIdSafe(), active);
    var page = settingsAdmin.search(criteria, pageRequest);
    return page.items().stream().map(setting -> new com.tchalanet.server.features.tenantadmin.config.settings.model.AdminSettingRow(
        setting.id().value().toString(),
        setting.namespace(),
        setting.settingKey(),
        setting.valueType().name(),
        setting.settingValue(),
        setting.level().name(),
        setting.active()
    )).toList();
  }

  public UpsertTenantSettingResult upsert(TchRequestContext ctx, com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingRequest req) {
    var tenantId = ctx.tenantIdSafe();
    var created = settingsAdmin.create(new com.tchalanet.server.catalog.settings.internal.web.model.CreateSettingRequest(
        req.namespace(), req.settingKey(), req.settingValue(), SettingValueType.STRING, SettingLevel.TENANT, tenantId, null, null
    ));

    return new UpsertTenantSettingResult(created.id() == null ? null : created.id().value().toString());
  }

  public void delete(TchRequestContext ctx, com.tchalanet.server.common.types.id.SettingId id) {
    settingsAdmin.delete(id);
  }
}
