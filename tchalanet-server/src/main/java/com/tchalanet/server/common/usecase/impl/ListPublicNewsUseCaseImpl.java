package com.tchalanet.server.common.usecase.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.common.usecase.ListPublicNewsUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementation that reads news JSON from Redis at key `news.redis.key` and also admin items at
 * `news.admin.key`. Returns combined list (admin items first) up to `news.public.max`.
 */
@Component
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListPublicNewsUseCaseImpl implements ListPublicNewsUseCase {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  @Value("${tch.news.redis.key:${news.redis.key:tch:public:news}}")
  private String newsRedisKey;

  @Value("${tch.news.admin.key:${news.admin.key:tch:public:news:admin}}")
  private String newsAdminKey;

  @Value("${tch.news.public.max:${news.public.max:20}}")
  private int publicMax;

  @Value("${tch.news.max-items:10}")
  private int maxItems;

  @Override
  public List<Map<String, Object>> listPublicNews() {
    try {
      List<Map<String, Object>> result = new ArrayList<>();

      // admin items first
      String adminJson = redis.opsForValue().get(newsAdminKey);
      if (adminJson != null && !adminJson.isBlank()) {
        var adminList =
            objectMapper.readValue(adminJson, new TypeReference<List<Map<String, Object>>>() {});
        result.addAll(adminList);
      }

      // feed items next
      String raw = redis.opsForValue().get(newsRedisKey);
      if (raw != null && !raw.isBlank()) {
        var feedList =
            objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
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
