package com.tchalanet.server.news.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.news.application.ports.in.RefreshPublicNewsUseCase;
import com.tchalanet.server.news.domain.model.NewsArticle;
import com.tchalanet.server.news.domain.ports.out.NewsProviderPort;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service; // Changed from @Component to @Service

/**
 * Refresh public news by fetching from an external provider and writing JSON into Redis. Normalizes
 * items to the expected schema: { title, link, published_at, summary, image }
 */
@Service // Changed from @Component to @Service
@RequiredArgsConstructor
@Slf4j
public class RefreshPublicNewsService implements RefreshPublicNewsUseCase {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final NewsProviderPort newsProvider;

  @Value("${tch.news.redis.key:${news.redis.key:tch:public:news}}")
  private String newsRedisKey;

  @Value("${tch.news.ttl.hours:${news.ttl.hours:24}}")
  private long newsTtlHours;

  @Override
  public void refreshNews() { // Renamed method to refreshNews
    try {
      List<NewsArticle> raw = newsProvider.fetchLatestNews(); // Changed to NewsArticle
      List<Map<String, Object>> normalized = raw.stream().map(this::normalize).toList();
      var json = objectMapper.writeValueAsString(normalized);
      redis.opsForValue().set(newsRedisKey, json, Duration.ofHours(newsTtlHours));
      log.info(
          "Refreshed public news ({} items) into key {} with ttl {}h",
          normalized.size(),
          newsRedisKey,
          newsTtlHours);
    } catch (Exception e) {
      log.error("Failed to refresh public news: {}", e.getMessage(), e);
    }
  }

  private Map<String, Object> normalize(NewsArticle article) { // Changed to NewsArticle
    Map<String, Object> out = new HashMap<>();
    out.put("id", article.id());
    out.put("title", article.title());
    out.put("content", article.content());
    out.put("url", article.url());
    // Add other fields from NewsArticle as needed
    return out;
  }
}
