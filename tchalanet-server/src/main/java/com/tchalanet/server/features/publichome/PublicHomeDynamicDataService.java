package com.tchalanet.server.features.publichome;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.HeroBlock;
import com.tchalanet.server.features.pagemodel.shared.block.NewsBlock;
import com.tchalanet.server.features.pagemodel.shared.block.PlansBlock;
import com.tchalanet.server.features.pagemodel.shared.block.PublicPageDynamicPayload;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// explicit imports for providers (same package) to avoid resolution issues
import com.tchalanet.server.features.publichome.dynamic.PublicFeaturesProvider;
import com.tchalanet.server.features.publichome.dynamic.HeroProvider;
import com.tchalanet.server.features.pagemodel.shared.dynamic.PlansProvider;
import com.tchalanet.server.features.pagemodel.shared.dynamic.NewsProvider;
import com.tchalanet.server.features.pagemodel.shared.dynamic.ResultsByGameProvider;

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
            sources = pageModel.content().widgets().values().stream()
                .filter(Objects::nonNull)
                .map(w -> w.binding())
                .filter(Objects::nonNull)
                .filter(b -> "dynamic".equalsIgnoreCase(b.mode()))
                .map(b -> b.source())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }

        HeroBlock hero = sources.contains("hero") ? heroProvider.buildHeroBlock(pageModel, currentLang) : null;
        var features = sources.contains("features") ? featuresProvider.buildFeaturesBlock(pageModel, currentLang) : null;
        PlansBlock plans = sources.contains("plans") ? plansProvider.buildPlansBlock(pageModel, currentLang) : null;
        NewsBlock news = sources.contains("news") ? newsProvider.buildNewsBlock(pageModel, currentLang) : null;
        ResultsByGameBlock resultsByGame = sources.contains("results_by_game") ? resultsProvider.buildResultsByGameBlock(pageModel, currentLang) : null;

        return new PublicPageDynamicPayload(hero, features, plans, news, resultsByGame);
    }
}
