package com.tchalanet.server.features.news.infra;

import com.tchalanet.server.features.news.domain.model.NewsArticle;
import com.tchalanet.server.features.news.domain.ports.out.NewsProviderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsProviderHttpAdapter implements NewsProviderPort {

  @Override
  public List<NewsArticle> fetchLatestNews() {
    log.warn(
        "NewsProviderHttpAdapter is a placeholder and does not implement actual HTTP fetching.");
    return List.of(
        new NewsArticle("1", "Sample News 1", "Content 1", "https://example.com/news1"),
        new NewsArticle("2", "Sample News 2", "Content 2", "https://example.com/news2"));
  }
}
