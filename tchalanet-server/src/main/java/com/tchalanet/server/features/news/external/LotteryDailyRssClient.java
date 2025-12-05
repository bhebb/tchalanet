package com.tchalanet.server.features.news.external;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.features.news.config.NewsConfigProperties;
import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsFeedSnapshot;
import com.tchalanet.server.features.news.shared.port.NewsProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LotteryDailyRssClient implements NewsProviderPort {

    private final RomeNewsMapper romeNewsMapper;
    private final NewsConfigProperties newsConfigProperties;

    /**
     * Récupère le flux RSS brut via ROME (SyndFeed).
     */
    public SyndFeed fetchRawFeed() {
        String feedUrl = resolveFeedUrl();
        try {
            var url = URL.of(URI.create(feedUrl), null);
            var input = new SyndFeedInput();
            return input.build(new XmlReader(url));
        } catch (Exception e) {
            log.error("Failed to fetch RSS feed from {}: {}", feedUrl, e.getMessage(), e);
            throw ProblemRestException.unprocessable("Failed to fetch/parse RSS feed from " + feedUrl);
        }
    }

    /**
     * Récupère un snapshot complet du flux à l'instant T, mappé sur notre modèle domaine.
     */
    @Override
    public LotteryNewsFeedSnapshot fetchLatestNews() {
        try {
            SyndFeed feed = fetchRawFeed();
            return romeNewsMapper.map(feed);
        } catch (Exception e) {
            log.error("Failed to fetch or map Lottery RSS feed: {}", e.getMessage(), e);
            return new LotteryNewsFeedSnapshot(Instant.now(), List.of());
        }
    }

    private String resolveFeedUrl() {
        String url = null;
        if (newsConfigProperties.getProvider() != null) {
            url = newsConfigProperties.getProvider().getUrl();
        }
        if (url == null || url.isBlank()) {
            // fallback v1
            url = "https://lotterydaily.com/feed/";
        }
        return url;
    }

}
