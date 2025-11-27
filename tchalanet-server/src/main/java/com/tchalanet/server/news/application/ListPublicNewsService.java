package com.tchalanet.server.news.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.news.application.ports.in.ListPublicNewsUseCase;
import com.tchalanet.server.news.domain.model.NewsArticle;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service; // Changed from @Component to @Service

/**
 * Implementation that reads news JSON from Redis at key `news.redis.key` and also admin items at
 * `news.admin.key`. Returns combined list (admin items first) up to `news.public.max`.
 */
@Service // Changed from @Component to @Service
@RequiredArgsConstructor
@Slf4j
public class ListPublicNewsService implements ListPublicNewsUseCase {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  @Value("${tch.news.redis.key:${news.redis.key:tch:public:news}}")
  private String newsRedisKey;

  @Value("${tch.news.admin.key:${news.admin.key:tch:public:news:admin}}")
  private String newsAdminKey;

  @Value("${tch.news.public.max:${news.public.max:20}}")
  private int publicMax;

  @Override
  public List<NewsArticle> listNews() {
    try {
      List<NewsArticle> result = new ArrayList<>();

      // admin items first
      String adminJson = redis.opsForValue().get(newsAdminKey);
      if (adminJson != null && !adminJson.isBlank()) {
        var adminList =
            objectMapper.readValue(
                adminJson, new TypeReference<List<NewsArticle>>() {}); // Changed to NewsArticle
        result.addAll(adminList);
      }

      // feed items next
      String raw = redis.opsForValue().get(newsRedisKey);
      if (raw != null && !raw.isBlank()) {
        var feedList =
            objectMapper.readValue(
                raw, new TypeReference<List<NewsArticle>>() {}); // Changed to NewsArticle
        result.addAll(feedList);
      }

      if (result.size() > publicMax) return result.subList(0, publicMax);
      return result;
    } catch (Exception e) {
      log.warn("Failed to read/parse public news from Redis: {}", e.getMessage(), e);
      return List.of();
    }
  }
}
