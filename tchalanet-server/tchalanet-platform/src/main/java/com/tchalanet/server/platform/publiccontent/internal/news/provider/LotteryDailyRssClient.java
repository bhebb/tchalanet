package com.tchalanet.server.platform.publiccontent.internal.news.provider;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentConfigProperties;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentItem;
import java.net.URI;
import java.net.URL;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LotteryDailyRssClient implements NewsProvider {

  private static final String DEFAULT_FEED_URL = "https://lotterydaily.com/feed/";

  private final RomeNewsMapper romeNewsMapper;
  private final PublicContentConfigProperties props;

  public SyndFeed fetchRawFeed() {
    String feedUrl = resolveFeedUrl();
    try {
      var url = URL.of(URI.create(feedUrl), null);
      return new SyndFeedInput().build(new XmlReader(url));
    } catch (Exception e) {
      log.error("publiccontent: failed to fetch RSS feed from {}: {}", feedUrl, e.getMessage(), e);
      throw ProblemRestException.unprocessable("Failed to fetch/parse RSS feed from " + feedUrl);
    }
  }

  @Override
  public List<PublicContentItem> fetchLatestNews() {
    try {
      return romeNewsMapper.map(fetchRawFeed());
    } catch (Exception e) {
      log.error("publiccontent: failed to map RSS feed: {}", e.getMessage(), e);
      return List.of();
    }
  }

  private String resolveFeedUrl() {
    if (props.provider() != null && props.provider().url() != null) {
      return props.provider().url().toString();
    }
    return DEFAULT_FEED_URL;
  }
}
