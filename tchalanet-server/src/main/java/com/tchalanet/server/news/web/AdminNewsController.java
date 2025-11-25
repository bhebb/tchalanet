package com.tchalanet.server.news.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.usecase.RefreshPublicNewsUseCase;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoints to add/remove news items in Redis. Protected by security elsewhere (expect
 * SUPER_ADMIN role in production).
 */
@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminNewsController {

  private final ObjectMapper objectMapper;
  private final RefreshPublicNewsUseCase refreshUseCase;

  @Value("${news.redis.key:tch:public:news}")
  private String newsKey;

  @Value("${news.admin.key:tch:public:news:admin}")
  private String newsAdminKey;

  private final org.springframework.data.redis.core.StringRedisTemplate redis;

  @PostMapping
  public ResponseEntity<String> add(@RequestBody Map<String, Object> item) {
    try {
      // enrich with createdAt
      item.put("created_at", OffsetDateTime.now().toString());
      // read existing admin items
      String json = redis.opsForValue().get(newsAdminKey);
      List<Map<String, Object>> list = new ArrayList<>();
      if (json != null && !json.isBlank())
        list =
            objectMapper.readValue(
                json,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
      list.add(0, item); // prepend
      redis.opsForValue().set(newsAdminKey, objectMapper.writeValueAsString(list));
      log.info("Admin added news item, total admin items={}", list.size());
      return ResponseEntity.ok("ok");
    } catch (Exception e) {
      log.error("Failed to add admin news", e);
      return ResponseEntity.status(500).body("error");
    }
  }

  @GetMapping
  public List<Map<String, Object>> listAdmin() {
    try {
      String json = redis.opsForValue().get(newsAdminKey);
      if (json == null || json.isBlank()) return List.of();
      return objectMapper.readValue(
          json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception e) {
      log.error("Failed to read admin news", e);
      return List.of();
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<String> refreshNow() {
    try {
      refreshUseCase.refresh();
      return ResponseEntity.ok("refreshed");
    } catch (Exception e) {
      log.error("Failed to refresh news on admin request", e);
      return ResponseEntity.status(500).body("error");
    }
  }
}
