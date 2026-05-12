package com.tchalanet.server.features.news.shared;

import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsFeedSnapshot;
import java.util.List;

public interface NewsCache {

  LotteryNewsFeedSnapshot getLatestNews(String cacheKey);

  void putLatestNews(String cacheName, LotteryNewsFeedSnapshot snapshot);

  void addHidden(String cacheKey, String articleId);

  List<String> getHidden(String cacheKey);

  void removeHidden(String cacheKey, String articleId);
}
