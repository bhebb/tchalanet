package com.tchalanet.server.catalog.i18n.api;

import com.tchalanet.server.catalog.i18n.api.model.CreateI18nOverrideAdminRequest;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.common.types.id.I18nOverrideId;

public interface I18nOverridesAdminCatalog {

  I18nOverrideView create(CreateI18nOverrideAdminRequest request);

  void delete(I18nOverrideId id);
}
