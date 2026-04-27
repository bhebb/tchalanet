package com.tchalanet.server.features.news.publicnews;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import com.tchalanet.server.features.news.shared.service.NewsAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicNewsService {

    private final NewsAggregationService aggregationService;

    private final Clock clock;

    /**
     * Liste complète (internes + externes), triée desc par date. Utilisée pour /api/public/news (page
     * complète).
     */
    public List<LotteryNewsModels.LotteryNewsArticle> listAll() {
        return aggregationService.aggregateOrdered(Instant.now(clock));
    }
}
