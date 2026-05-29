package com.tchalanet.server.platform.publiccontent.internal;

import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentItem;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentQueryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicContentApiService implements PublicContentApi {

  private final PublicContentQueryService queryService;

  @Override
  public List<PublicContentItemView> listPublicHomeNews(int limit) {
    return toViews(queryService.listForSurface(PublicContentSurface.PUBLIC_HOME, limit));
  }

  @Override
  public List<PublicContentItemView> listTenantAdminDashboardNews(int limit) {
    return toViews(queryService.listForSurface(PublicContentSurface.TENANT_ADMIN_DASHBOARD, limit));
  }

  @Override
  public List<PublicContentItemView> listPlatformAdminDashboardNews(int limit) {
    return toViews(queryService.listForSurface(PublicContentSurface.PLATFORM_ADMIN_DASHBOARD, limit));
  }

  @Override
  public List<PublicContentItemView> listPosDashboardNews(int limit) {
    return toViews(queryService.listForSurface(PublicContentSurface.POS_DASHBOARD, limit));
  }

  private List<PublicContentItemView> toViews(List<PublicContentItem> items) {
    return items.stream().map(this::toView).toList();
  }

  private PublicContentItemView toView(PublicContentItem item) {
    return new PublicContentItemView(
        toUuid(item.id()),
        item.title(),
        item.content(),
        item.imageUrl(),
        item.sourceUrl() != null ? item.sourceUrl().toString() : null,
        item.sourceType(),
        item.publishedAt());
  }

  private static UUID toUuid(String id) {
    if (id == null) return null;
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(id.getBytes());
    }
  }
}
