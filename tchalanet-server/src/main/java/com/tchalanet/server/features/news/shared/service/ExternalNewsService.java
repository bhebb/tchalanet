package com.tchalanet.server.features.news.shared.service;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import com.tchalanet.server.features.news.shared.port.NewsCachePort;
import com.tchalanet.server.features.news.shared.port.NewsProviderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalNewsService {

  private final NewsProviderPort newsProvider;
  private final NewsCachePort newsCachePort;
  private final CacheKeyBuilder cacheKeyBuilder;

  public LotteryNewsModels.LotteryNewsFeedSnapshot fetchSnapshot() {
    try {

      // v1 : le provider retourne une liste d’articles de type LotteryNewsArticle
      var newsFeedSnapshot = newsProvider.fetchLatestNews();

      newsCachePort.putLatestNews(cacheKeyBuilder.newsExternalKey(), newsFeedSnapshot);
      log.info(
          "Refreshed public news snapshot into news cache ({} articles)",
          newsFeedSnapshot.articles().size());
      return newsFeedSnapshot;
    } catch (Exception e) {
      log.error("Failed to refresh public news: {}", e.getMessage(), e);
    }
    return LotteryNewsModels.LotteryNewsFeedSnapshot.empty();
  }

  /** Raccourci pratique : la liste d’articles du snapshot courant. */
  public List<LotteryNewsModels.LotteryNewsArticle> fetchArticles() {
    return fetchSnapshot().articles();
  }
}
