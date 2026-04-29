package com.tchalanet.server.features.tenantadmin.config.i18n;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.catalog.i18n.internal.write.I18nOverridesAdminService;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.AdminI18nRow;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.UpsertI18nOverrideRequest;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.UpsertI18nOverrideResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantAdminI18nService {

  private final I18nOverridesCatalog i18nOverridesCatalog;
  private final I18nOverridesAdminService adminService;

  public TchPage<AdminI18nRow> search(TchRequestContext ctx, String locale, String q, Boolean active, TchPageRequest pageReq) {
    var criteria = new SearchI18nOverridesCriteria(
        I18nOverrideLevel.TENANT,
        locale,
        q,
        active,
        ctx.tenantIdSafe().value(),
        "active");
    var page = i18nOverridesCatalog.search(criteria, pageReq);
    var rows = page.items().stream()
        .map(view -> new AdminI18nRow(
            view.id().value().toString(),
            view.locale(),
            view.i18nKey(),
            view.i18nValue(),
            view.level().name(),
            view.active()))
        .toList();
    return TchPage.of(
        rows,
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages(),
        page.last(),
        page.hasNext(),
        page.hasPrevious());
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
    return i18nOverridesCatalog.resolveLocale(locale, ctx);
  }
}
