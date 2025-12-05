package com.tchalanet.server.features.news.shared.service;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsAggregationService {

    private final InternalNewsService internalNewsService;
    private final ExternalNewsService externalNewsService;
    private final HiddenNewsService hiddenNewsService;

    /**
     * Retourne la liste agrégée interne+externe, triée, avec internes en premier.
     */
    public List<LotteryNewsModels.LotteryNewsArticle> aggregateOrdered(Instant now) {

        var hiddenIds = hiddenNewsService.getHiddenIds();

        // helper
        var notHidden = (java.util.function.Predicate<LotteryNewsModels.LotteryNewsArticle>) a ->
            !hiddenIds.contains(a.id());

        // 1) internes PUBLISHED, filtrées, triées desc
        var internal = internalNewsService.findPublished(now).stream()
            .filter(notHidden)
            .sorted(Comparator.comparing(LotteryNewsModels.LotteryNewsArticle::publishedAt).reversed())
            .toList();

        // 2) externes, filtrées, triées desc
        var external = externalNewsService.fetchArticles().stream()
            .filter(notHidden)
            .sorted(Comparator.comparing(LotteryNewsModels.LotteryNewsArticle::publishedAt).reversed())
            .toList();

        // 3) concat : internes d’abord, externes ensuite
        var result = new ArrayList<LotteryNewsModels.LotteryNewsArticle>(
            internal.size() + external.size()
        );
        result.addAll(internal);
        result.addAll(external);

        return result;
    }
}
