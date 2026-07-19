package com.tchalanet.server.platform.publiccontent.internal.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentRefreshResult;
import com.tchalanet.server.platform.publiccontent.internal.news.provider.NewsProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalRssNewsServiceTest {

  @Test
  void refreshReportsFreshSnapshotWhenProviderSucceeds() {
    var provider = mock(NewsProvider.class);
    var cache = mock(PublicContentCache.class);
    var keys = mock(CacheKeyBuilder.class);
    var items = List.<PublicContentItem>of();
    when(provider.fetchLatestNews()).thenReturn(items);
    when(keys.newsExternalKey()).thenReturn("publiccontent:external");
    var service = new ExternalRssNewsService(provider, cache, keys);

    var result = service.refreshExternalSnapshot();

    assertThat(result).isEqualTo(PublicContentRefreshResult.refreshed(0));
    verify(cache).putExternalSnapshot("publiccontent:external", items);
  }

  @Test
  void refreshReportsStaleCacheWhenProviderFails() {
    var provider = mock(NewsProvider.class);
    var cache = mock(PublicContentCache.class);
    var keys = mock(CacheKeyBuilder.class);
    when(provider.fetchLatestNews()).thenThrow(new IllegalStateException("provider unavailable"));
    when(keys.newsExternalKey()).thenReturn("publiccontent:external");
    when(cache.getExternalSnapshot("publiccontent:external")).thenReturn(List.of());
    var service = new ExternalRssNewsService(provider, cache, keys);

    var result = service.refreshExternalSnapshot();

    assertThat(result).isEqualTo(PublicContentRefreshResult.staleCacheRetained(0));
  }
}
