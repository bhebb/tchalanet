package com.tchalanet.server.features.news.shared.service;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.features.news.shared.port.NewsCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class HiddenNewsService {

    private final NewsCachePort newsCachePort;
    private final CacheKeyBuilder cacheKeyBuilder;

    private String hiddenKey() {
        return cacheKeyBuilder.newsHiddenKey();
    }

    public List<String> getHiddenIds() {
        return newsCachePort.getHidden(hiddenKey());
    }

    public void hide(String articleId) {
        newsCachePort.addHidden(hiddenKey(), articleId);
        log.info("News {} hidden in overlay cache", articleId);
    }

    public void show(String articleId) {
        newsCachePort.removeHidden(hiddenKey(), articleId);
        log.info("News {} unhidden in overlay cache", articleId);
    }

    public boolean isHidden(String articleId) {
        return getHiddenIds().contains(articleId);
    }

}
