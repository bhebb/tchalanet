package com.tchalanet.server.common.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.usecase.NewsProviderPort;
import com.tchalanet.server.common.usecase.RefreshPublicNewsUseCase;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Refresh public news by fetching from an external provider and writing JSON into Redis. Normalizes
 * items to the expected schema: { title, link, published_at, summary, image }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshPublicNewsUseCaseImpl implements RefreshPublicNewsUseCase {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final NewsProviderPort newsProvider;

  @Value("${tch.news.redis.key:${news.redis.key:tch:public:news}}")
  private String newsRedisKey;

  @Value("${tch.news.ttl.hours:${news.ttl.hours:24}}")
  private long newsTtlHours;

  @Override
  public void refresh() {
    try {
      List<Map<String, Object>> raw = newsProvider.fetchLatest();
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

  private Map<String, Object> normalize(Map<String, Object> src) {
    Map<String, Object> out = new HashMap<>();
    // title
    out.put("title", strOf(src, "title", "headline", "name", "heading"));
    // link
    out.put("link", strOf(src, "link", "url", "href", "uri"));
    // summary / description
    out.put("summary", strOf(src, "summary", "description", "body", "excerpt"));
    // image
    out.put("image", strOf(src, "image", "imageUrl", "thumbnail", "media"));
    // published_at - try to normalize to ISO-8601
    var publishedRaw = strOf(src, "published_at", "publishedAt", "date", "pubDate", "published");
    var publishedIso = normalizeDate(publishedRaw);
    out.put("published_at", publishedIso != null ? publishedIso : publishedRaw);
    return out;
  }

  private String strOf(Map<String, Object> src, String... keys) {
    for (var k : keys) {
      if (src.containsKey(k) && src.get(k) != null) {
        var v = src.get(k);
        if (v instanceof String s && !s.isBlank()) return s;
        // if object with url field
        if (v instanceof Map<?, ?> m) {
          Object possible = m.containsKey("url") ? m.get("url") : m.get("src");
          if (possible instanceof String ps && !ps.isBlank()) return ps;
        }
      }
      // try case-insensitive key
      for (var entry : src.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(k)
            && entry.getValue() instanceof String s
            && !s.isBlank()) return s;
      }
    }
    return null;
  }

  private String normalizeDate(String raw) {
    if (raw == null) return null;
    try {
      var odt = OffsetDateTime.parse(raw);
      return odt.toString();
    } catch (DateTimeParseException e) {
      // try common formats or let it be
      try {
        var odt = OffsetDateTime.parse(raw + "Z");
        return odt.toString();
      } catch (Exception ex) {
        return null;
      }
    }
  }
}
