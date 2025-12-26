package com.tchalanet.server.features.news.publicnews;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import com.tchalanet.server.features.news.shared.service.NewsAggregationService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicNewsService {

  private final NewsAggregationService aggregationService;

  /**
   * Liste complète (internes + externes), triée desc par date. Utilisée pour /api/public/news (page
   * complète).
   */
  public List<LotteryNewsModels.LotteryNewsArticle> listAll() {
    return aggregationService.aggregateOrdered(Instant.now());
  }

  /** N articles pour la home publique. Utilisée par features.publichome. */
  public List<LotteryNewsModels.LotteryNewsArticle> listForHome(int limit) {
    var all = aggregationService.aggregateOrdered(Instant.now());
    return all.stream().limit(limit).toList();
  }
}
