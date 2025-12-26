package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import com.tchalanet.server.features.news.shared.service.NewsAggregationService;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.NewsBlock;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedNewsProvider implements NewsProvider {

  private final NewsAggregationService newsAggregationService;
  private final Clock clock; // injecté pour testabilité

  @Override
  public NewsBlock buildNewsBlock(PageModel pageModel, String currentLang) {
    // 1) Récupérer la config du widget (ex: max_items)
    int maxItems = resolveMaxItems(pageModel);

    // 2) Appeler le service métier (idéalement avec la langue si supportée)
    Instant now = Instant.now(clock);
    var articles = newsAggregationService.aggregateOrdered(now /*, currentLang */);

    // 3) Mapper vers le bloc, en respectant maxItems
    var items = toNewsItems(articles, maxItems);

    return new NewsBlock(items);
  }

  private int resolveMaxItems(PageModel pageModel) {
    // À affiner selon ta structure de PageModel.Widget
    var widgets = pageModel.content().widgets();
    var newsWidget = widgets.get("home.news");
    if (newsWidget == null || newsWidget.props() == null) {
      return 4; // valeur par défaut
    }

    Object rawMax = newsWidget.props().get("max_items");
    if (rawMax instanceof Number n) {
      return n.intValue();
    }

    return 4; // fallback
  }

  private List<NewsBlock.NewsItem> toNewsItems(
      List<LotteryNewsModels.LotteryNewsArticle> articles, int maxItems) {
    if (articles == null || articles.isEmpty()) {
      return List.of();
    }

    return articles.stream()
        .filter(Objects::nonNull)
        .limit(maxItems)
        .map(NewsBlock.NewsItem::fromDomain) // mapper dans le record
        .toList();
  }
}
