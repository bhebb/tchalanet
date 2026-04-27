package com.tchalanet.server.catalog.drawchannel.internal.write;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.drawchannel.internal.cache.DrawChannelCacheNames;
import com.tchalanet.server.catalog.drawchannel.internal.mapper.DrawChannelGameMapper;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelGameEntity;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelGameRepository;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawChannelGameAdminService {

  private final DrawChannelGameRepository repository;
  private final DrawChannelGameMapper mapper;
  private final JsonUtils jsonUtils;
  private final JdbcTemplate jdbc;

  @Transactional
  @CacheEvict(cacheNames = {DrawChannelCacheNames.BY_TENANT, DrawChannelCacheNames.BY_ID, DrawChannelCacheNames.BY_TENANT_GAME_MAP}, allEntries = true)
  public com.tchalanet.server.catalog.drawchannel.internal.web.model.DrawChannelGameResponse upsert(
      TenantId tenantId, DrawChannelId channelId, GameId gameId, boolean enabled, JsonNode flags) {

    var existing = repository.findByTenantIdAndDrawChannelIdAndGameIdAndDeletedAtIsNull(
        tenantId.value(), channelId.value(), gameId.value());

    var normalizedFlags = flags;
    if (normalizedFlags == null) normalizedFlags = jsonUtils.emptyObjectNode();
    else if (normalizedFlags.isTextual()) normalizedFlags = jsonUtils.parse(normalizedFlags.asText());

    if (existing.isPresent()) {
      var e = existing.get();
      e.setEnabled(enabled);
      e.setFlags(normalizedFlags);
      e.setUpdatedAt(Instant.now());
      var saved = repository.save(e);
      return mapper.toResponse(saved);
    } else {
      var e = new DrawChannelGameEntity();
      e.setId(UUID.randomUUID());
      e.setTenantId(tenantId.value());
      e.setDrawChannelId(channelId.value());
      e.setGameId(gameId.value());
      e.setEnabled(enabled);
      e.setFlags(normalizedFlags);
      e.setCreatedAt(Instant.now());
      e.setUpdatedAt(Instant.now());
      e.setVersion(0);
      var saved = repository.save(e);
      return mapper.toResponse(saved);
    }
  }

  @Transactional
  @CacheEvict(cacheNames = {DrawChannelCacheNames.BY_TENANT, DrawChannelCacheNames.BY_ID, DrawChannelCacheNames.BY_TENANT_GAME_MAP}, allEntries = true)
  public com.tchalanet.server.catalog.drawchannel.internal.web.model.DrawChannelGameResponse update(
      DrawChannelId channelId, GameId gameId, com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelGameRequest req) {
    var existing = repository.findByTenantIdAndDrawChannelIdAndGameIdAndDeletedAtIsNull(null, channelId.value(), gameId.value())
        .orElseThrow(() -> new IllegalArgumentException("Association not found"));

    if (req.enabled() != null) existing.setEnabled(req.enabled());
    if (req.flags() != null) {
      var flags = req.flags();
      if (flags.isTextual()) flags = jsonUtils.parse(flags.asText());
      existing.setFlags(flags);
    }
    existing.setUpdatedAt(Instant.now());
    var saved = repository.save(existing);
    return mapper.toResponse(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {DrawChannelCacheNames.BY_TENANT, DrawChannelCacheNames.BY_ID, DrawChannelCacheNames.BY_TENANT_GAME_MAP}, allEntries = true)
  public void softDelete(DrawChannelId channelId, GameId gameId) {
    var existing = repository.findByTenantIdAndDrawChannelIdAndGameIdAndDeletedAtIsNull(null, channelId.value(), gameId.value());
    if (existing.isPresent()) {
      var e = existing.get();
      e.setDeletedAt(Instant.now());
      e.setUpdatedAt(Instant.now());
      repository.save(e);
    }
  }

  @Transactional
  @CacheEvict(cacheNames = {DrawChannelCacheNames.BY_TENANT, DrawChannelCacheNames.BY_ID, DrawChannelCacheNames.BY_TENANT_GAME_MAP}, allEntries = true)
  public List<com.tchalanet.server.catalog.drawchannel.internal.web.model.DrawChannelGameResponse> bulkUpsert(
      TenantId tenantId, DrawChannelId channelId, List<com.tchalanet.server.catalog.drawchannel.internal.web.model.CreateDrawChannelGameRequest> items) {
    if (items == null || items.isEmpty()) return List.of();

    // Prepare batch values
    String sql = "INSERT INTO draw_channel_game (id, tenant_id, draw_channel_id, game_id, enabled, flags, created_at, updated_at, version) VALUES ";
    StringBuilder sb = new StringBuilder(sql);
    List<Object> params = new ArrayList<>();
    int idx = 0;
    for (var it : items) {
      if (idx > 0) sb.append(',');
      sb.append("(?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)");
      params.add(UUID.randomUUID()); // id
      params.add(tenantId.value());
      params.add(channelId.value());
      params.add(it.gameId().value());
      params.add(it.enabled());
      params.add(jsonUtils.toJson(it.flags() == null ? jsonUtils.emptyObjectNode() : it.flags()));
      Timestamp now = Timestamp.from(Instant.now());
      params.add(now);
      params.add(now);
      params.add(0);
      idx++;
    }

    sb.append(" ON CONFLICT (tenant_id, draw_channel_id, game_id) DO UPDATE SET enabled = EXCLUDED.enabled, flags = EXCLUDED.flags, updated_at = now(), version = draw_channel_game.version + 1 WHERE draw_channel_game.deleted_at IS NULL");

    jdbc.update(sb.toString(), params.toArray());

    // fetch saved entities
    var gameIds = items.stream().map(i -> i.gameId().value()).distinct().collect(Collectors.toList());
    var saved = repository.findByTenantIdAndDrawChannelIdAndGameIdInAndDeletedAtIsNull(tenantId.value(), channelId.value(), gameIds);
    return mapper.toResponses(saved);
  }
}
