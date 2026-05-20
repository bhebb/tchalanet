package com.tchalanet.server.catalog.settings.api;

import com.tchalanet.server.catalog.settings.api.model.CreateSettingAdminRequest;
import com.tchalanet.server.catalog.settings.api.model.SearchSettingsAdminCriteria;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

public interface SettingsAdminCatalog {

  TchPage<SettingView> search(SearchSettingsAdminCriteria criteria, TchPageRequest pageRequest);

  SettingView create(CreateSettingAdminRequest request);

  void delete(SettingId id);
}
