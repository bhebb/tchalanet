package com.tchalanet.server.features.publichome_back;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.PublicPageDynamicPayload;
import com.tchalanet.server.features.pagemodel_backup.shared.dynamic.NewsProvider;
import com.tchalanet.server.features.pagemodel_backup.shared.dynamic.PlansProvider;
import com.tchalanet.server.features.pagemodel_backup.shared.dynamic.ResultsByGameProvider;
import com.tchalanet.server.features.publichome_back.dynamic.HeroProvider;
import com.tchalanet.server.features.publichome_back.dynamic.PublicFeaturesProvider;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicHomeDynamicDataService {

  private final HeroProvider heroProvider;
  private final PublicFeaturesProvider featuresProvider;
  private final PlansProvider plansProvider;
  private final NewsProvider newsProvider;
  private final ResultsByGameProvider resultsProvider;

  public PublicPageDynamicPayload buildDynamicData(PageModel pageModel, String currentLang) {
    // Collecte les sources dynamiques déclarées dans les widgets
    Set<String> sources = Set.of();
    if (pageModel != null && pageModel.content() != null && pageModel.content().widgets() != null) {
      sources =
          pageModel.content().widgets().values().stream()
              .filter(Objects::nonNull)
              .map(w -> w.binding())
              .filter(Objects::nonNull)
              .filter(b -> "dynamic".equalsIgnoreCase(b.mode()))
              .map(b -> b.source())
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
    }

    var hero =
        sources.contains("hero") ? heroProvider.buildHeroBlock(pageModel, currentLang) : null;
    var features =
        sources.contains("features")
            ? featuresProvider.buildFeaturesBlock(pageModel, currentLang)
            : null;
    var plans =
        sources.contains("plans") ? plansProvider.buildPlansBlock(pageModel, currentLang) : null;
    var news =
        sources.contains("news") ? newsProvider.buildNewsBlock(pageModel, currentLang) : null;
    var resultsByGame =
        sources.contains("results_by_game")
            ? resultsProvider.buildResultsBlock(pageModel, currentLang)
            : null;

    return new PublicPageDynamicPayload(hero, features, plans, news, resultsByGame);
  }
}
