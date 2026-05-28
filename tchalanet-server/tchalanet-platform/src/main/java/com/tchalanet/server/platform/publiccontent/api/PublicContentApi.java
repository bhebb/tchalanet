package com.tchalanet.server.platform.publiccontent.api;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;

import java.util.List;

public interface PublicContentApi {
  List<PublicContentItemView> listPublicHomeNews(int limit);
  List<PublicContentItemView> listTenantAdminDashboardNews(int limit);
  List<PublicContentItemView> listPlatformAdminDashboardNews(int limit);
  List<PublicContentItemView> listPosDashboardNews(int limit);
}
