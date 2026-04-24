package com.tchalanet.server.features.tenantadmin.config.settings;

import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.catalog.settings.internal.write.SettingsAdminService;
import com.tchalanet.server.common.context.TchRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAdminSettingsService {

  private final SettingsCatalog settingsCatalog;
  private final SettingsAdminService settingsAdmin;

  public List<com.tchalanet.server.features.tenantadmin.config.settings.model.AdminSettingRow> search(TchRequestContext ctx, String namespace, String settingKey, Boolean active) {
    var criteria = new ResolveSettingsCriteria(ctx.tenantIdSafe(), null, null, List.of(namespace == null ? "" : namespace));
    var resolved = settingsCatalog.resolve(criteria);
    return resolved.stream().map(r -> new com.tchalanet.server.features.tenantadmin.config.settings.model.AdminSettingRow(
        r.id() == null ? null : r.id().value().toString(), r.namespace(), r.settingKey(), r.valueType().name(), r.settingValue(), r.level().name(), r.active()
    )).collect(Collectors.toList());
  }

  public com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingResult upsert(TchRequestContext ctx, com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingRequest req) {
    // delegate to catalog write-side admin service. Force tenant level.
    var tenantId = ctx.tenantIdSafe();
    var created = settingsAdmin.create(new com.tchalanet.server.catalog.settings.internal.web.model.CreateSettingRequest(
        req.namespace(), req.settingKey(), req.settingValue(), com.tchalanet.server.catalog.settings.api.model.SettingValueType.TEXT, com.tchalanet.server.catalog.settings.api.model.SettingLevel.TENANT, tenantId, null, null
    ));

    return new com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingResult(created.id() == null ? null : created.id().value().toString());
  }

  public void delete(TchRequestContext ctx, com.tchalanet.server.common.types.id.SettingId id) {
    settingsAdmin.delete(id);
  }
}
