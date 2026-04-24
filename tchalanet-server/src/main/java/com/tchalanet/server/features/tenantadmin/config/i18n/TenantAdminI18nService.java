package com.tchalanet.server.features.tenantadmin.config.i18n;

import com.tchalanet.server.catalog.i18n.internal.write.I18nOverridesAdminService;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.AdminI18nRow;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.UpsertI18nOverrideRequest;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.UpsertI18nOverrideResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantAdminI18nService {

  private final I18nOverridesAdminService adminService;

  public List<AdminI18nRow> search(TchRequestContext ctx, String locale, String q, Boolean active) {
    throw new UnsupportedOperationException("I18n search facade not implemented yet");
  }

  public UpsertI18nOverrideResult upsert(TchRequestContext ctx, UpsertI18nOverrideRequest req) {
    var created = adminService.create(new com.tchalanet.server.catalog.i18n.internal.web.model.CreateI18nOverrideRequest(
        ctx.tenantIdSafe(), req.locale(), com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel.TENANT, req.i18nKey(), req.i18nValue()
    ));
    return new UpsertI18nOverrideResult(created.id() == null ? null : created.id().value().toString());
  }

  public void delete(TchRequestContext ctx, com.tchalanet.server.common.types.id.I18nOverrideId id) {
    adminService.delete(id);
  }

  public java.util.Map<String, String> resolvePreview(TchRequestContext ctx, String locale) {
    throw new UnsupportedOperationException("I18n resolve preview not implemented: delegate to catalog.i18n read service");
  }
}
